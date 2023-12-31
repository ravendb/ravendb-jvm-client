package net.ravendb.client.http;

import net.ravendb.client.exceptions.AllTopologyNodesDownException;
import net.ravendb.client.exceptions.RequestedNodeUnavailableException;
import net.ravendb.client.exceptions.database.DatabaseDoesNotExistException;
import net.ravendb.client.primitives.CleanCloseable;
import net.ravendb.client.primitives.Timer;
import org.apache.commons.lang3.ObjectUtils;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

public class NodeSelector implements CleanCloseable {

    private final ExecutorService executorService;
    private Timer _updateFastestNodeTimer;
    protected NodeSelectorState _state;

    public Topology getTopology() {
        return _state.topology;
    }

    public NodeSelector(Topology topology, ExecutorService executorService) {
        _state = new NodeSelectorState(topology);
        this.executorService = executorService;
    }

    public void onFailedRequest(int nodeIndex) {
        NodeSelectorState state = _state;
        if (nodeIndex < 0 || nodeIndex >= state.failures.length) {
            return; // probably already changed
        }

        state.failures[nodeIndex].incrementAndGet();
    }

    public boolean onUpdateTopology(Topology topology) {
        return onUpdateTopology(topology, false);
    }

    @SuppressWarnings("UnnecessaryLocalVariable")
    public boolean onUpdateTopology(Topology topology, boolean forceUpdate) {
        if (topology == null) {
            return false;
        }

        Long stateEtag = ObjectUtils.firstNonNull(_state.topology.getEtag(), 0L);
        Long topologyEtag = ObjectUtils.firstNonNull(topology.getEtag(), 0L);

        if (stateEtag >= topologyEtag && !forceUpdate) {
            return false;
        }

        NodeSelectorState state = new NodeSelectorState(topology, _state);

        _state = state;

        return true;
    }

    public CurrentIndexAndNode getRequestedNode(String nodeTag) {
        NodeSelectorState state = _state;
        List<ServerNode> serverNodes = state.getNodes();
        for (int i = 0; i < serverNodes.size(); i++) {
            if (serverNodes.get(i).getClusterTag().equals(nodeTag)) {
                return new CurrentIndexAndNode(i, serverNodes.get(i));
            }
        }

        if (state.getNodes().size() == 0) {
            throw new DatabaseDoesNotExistException("There are no nodes in the topology at all");
        }
        throw new RequestedNodeUnavailableException("Could not find requested node " + nodeTag);
    }

    public boolean nodeIsAvailable(int index) {
        return _state.failures[index].get() == 0;
    }

    public CurrentIndexAndNode getPreferredNode() {
        NodeSelectorState state = _state;
        return getPreferredNodeInternal(state);
    }

    public static CurrentIndexAndNode getPreferredNodeInternal(NodeSelectorState state) {
        AtomicInteger[] stateFailures = state.failures;
        List<ServerNode> serverNodes = state.getNodes();
        int len = Math.min(serverNodes.size(), stateFailures.length);
        for (int i = 0; i < len; i++) {
            if (stateFailures[i].get() == 0 && ServerNode.Role.MEMBER.equals(serverNodes.get(i).getServerRole())) {
                return new CurrentIndexAndNode(i, serverNodes.get(i));
            }
        }

        return unlikelyEveryoneFaultedChoice(state);
    }

    public AtomicInteger[] getNodeSelectorFailures() {
        return _state.failures;
    }

    private static CurrentIndexAndNode unlikelyEveryoneFaultedChoice(NodeSelectorState state) {
        // if there are all marked as failed, we'll chose the next (the one in CurrentNodeIndex)
        // one so the user will get an error (or recover :-) );
        if (state.getNodes().size() == 0) {
            throw new DatabaseDoesNotExistException("There are no nodes in the topology at all");
        }


        AtomicInteger[] stateFailures = state.failures;
        List<ServerNode> serverNodes = state.getNodes();
        int len = Math.min(serverNodes.size(), stateFailures.length);

        for (int i = 0; i < len; i++) {
            if (stateFailures[i].get() == 0) {
                return new CurrentIndexAndNode(i, serverNodes.get(i));
            }
        }

        return state.getNodeWhenEveryoneMarkedAsFaulted();
    }

    public CurrentIndexAndNode getNodeBySessionId(int sessionId) {
        NodeSelectorState state = _state;

        if (state.topology.getNodes().isEmpty()) {
            throw new AllTopologyNodesDownException("There are no nodes in the topology at all");
        }

        int index = Math.abs(sessionId % state.topology.getNodes().size());

        for (int i = index; i < state.failures.length; i++) {
            if (state.failures[i].get() == 0 && state.getNodes().get(i).getServerRole() == ServerNode.Role.MEMBER) {
                return new CurrentIndexAndNode(i, state.getNodes().get(i));
            }
        }

        for (int i = 0; i < index; i++) {
            if (state.failures[i].get() == 0 && state.getNodes().get(i).getServerRole() == ServerNode.Role.MEMBER) {
                return new CurrentIndexAndNode(i, state.getNodes().get(i));
            }
        }

        return getPreferredNode();
    }

    public CurrentIndexAndNode getFastestNode() {
        NodeSelectorState state = _state;
        if (state.failures[state.fastest].get() == 0 && state.getNodes().get(state.fastest).getServerRole() == ServerNode.Role.MEMBER) {
            return new CurrentIndexAndNode(state.fastest, state.getNodes().get(state.fastest));
        }

        // until new fastest node is selected, we'll just use the server preferred node or failover as usual
        scheduleSpeedTest();
        return getPreferredNode();
    }

    public void restoreNodeIndex(ServerNode node) {
        NodeSelectorState state = _state;
        int nodeIndex = state.getNodes().indexOf(node);
        if (nodeIndex == -1) {
            return;
        }

        state.failures[nodeIndex].set(0);
    }

    private void switchToSpeedTestPhase() {
        NodeSelectorState state = _state;

        if (!state.speedTestMode.compareAndSet(0, 1)) {
            return;
        }

        Arrays.fill(state.fastestRecords, 0);

        state.speedTestMode.incrementAndGet();
    }

    public boolean inSpeedTestPhase() {
        return _state.speedTestMode.get() > 1;
    }

    public void recordFastest(int index, ServerNode node) {
        NodeSelectorState state = _state;
        int[] stateFastest = state.fastestRecords;

        // the following two checks are to verify that things didn't move
        // while we were computing the fastest node, we verify that the index
        // of the fastest node and the identity of the node didn't change during
        // our check
        if (index < 0 || index >= stateFastest.length)
            return;

        if (node != state.getNodes().get(index)) {
            return;
        }

        if (++stateFastest[index] >= 10) {
            selectFastest(state, index);
            return;
        }

        if (state.speedTestMode.incrementAndGet() <= state.getNodes().size() * 10) {
            return;
        }

        //too many concurrent speed tests are happening
        int maxIndex = findMaxIndex(state);
        selectFastest(state, maxIndex);
    }

    private static int findMaxIndex(NodeSelectorState state) {
        int[] stateFastest = state.fastestRecords;
        int maxIndex = 0;
        int maxValue = 0;

        for (int i = 0; i < stateFastest.length; i++) {
            if (maxValue >= stateFastest[i]) {
                continue;
            }

            maxIndex = i;
            maxValue = stateFastest[i];
        }

        return maxIndex;
    }

    private void selectFastest(NodeSelectorState state, int index) {
        state.fastest = index;
        state.speedTestMode.set(0);

        scheduleSpeedTest();
    }

    private final Object _timerCreationLocker = new Object();

    public void scheduleSpeedTest() {
        if (_updateFastestNodeTimer != null) {
            return;
        }

        synchronized (_timerCreationLocker) {
            if (_updateFastestNodeTimer != null) {
                return ;
            }

            switchToSpeedTestPhase();

            _updateFastestNodeTimer = new Timer(this::switchToSpeedTestPhase, Duration.ofMinutes(1), Duration.ofMinutes(1), executorService);
        }
    }

    @Override
    public void close() {
        if (_updateFastestNodeTimer != null) {
            _updateFastestNodeTimer.close();
        }
    }

    private static class NodeSelectorState {
        public final Topology topology;
        public final AtomicInteger[] failures;
        public final int[] fastestRecords;
        public int fastest;
        public final AtomicInteger speedTestMode = new AtomicInteger(0);
        public int unlikelyEveryoneFaultedChoiceIndex;

        public NodeSelectorState(Topology topology) {
            this.topology = topology;
            this.failures = new AtomicInteger[topology.getNodes().size()];
            for (int i = 0; i < this.failures.length; i++) {
                this.failures[i] = new AtomicInteger(0);
            }
            this.fastestRecords = new int[topology.getNodes().size()];
            this.unlikelyEveryoneFaultedChoiceIndex = 0;
        }

        public NodeSelectorState(Topology topology, NodeSelectorState prevState) {
            this(topology);

            if (prevState.fastest < 0 || prevState.fastest >= prevState.getNodes().size()) {
                return ;
            }

            ServerNode fastestNode = prevState.getNodes().get(prevState.fastest);
            int index = 0;
            for (ServerNode node : topology.getNodes()) {
                if (node.getClusterTag().equals(fastestNode.getClusterTag())) {
                    fastest = index;
                    break;
                }
                index++;
            }

            // fastest node was not found in the new topology. enable speed tests
            if (index >= topology.getNodes().size()) {
                speedTestMode.set(2);
            } else {
                // we might be in the process of finding fastest node when we reorder the nodes, we don't want the tests to stop until we reach 10
                // otherwise, we want to stop the tests and they may be scheduled later on relevant topology change
                if (fastest < prevState.fastestRecords.length && prevState.fastestRecords[fastest] < 10) {
                    speedTestMode.set(prevState.speedTestMode.get());
                }
            }
        }

        public List<ServerNode> getNodes() {
            return topology.getNodes();
        }

        public CurrentIndexAndNode getNodeWhenEveryoneMarkedAsFaulted() {
            int index = unlikelyEveryoneFaultedChoiceIndex;
            this.unlikelyEveryoneFaultedChoiceIndex = (unlikelyEveryoneFaultedChoiceIndex + 1) % getNodes().size();

            return new CurrentIndexAndNode(index, getNodes().get(index));
        }
    }

}
