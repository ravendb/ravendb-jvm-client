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
    private NodeSelectorState _state;

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

        NodeSelectorState state = new NodeSelectorState(topology);

        _state = state;

        return true;
    }

    public CurrentIndexAndNode getRequestedNode(String nodeTag) {
        NodeSelectorState state = _state;
        List<ServerNode> serverNodes = state.nodes;
        for (int i = 0; i < serverNodes.size(); i++) {
            if (serverNodes.get(i).getClusterTag().equals(nodeTag)) {
                return new CurrentIndexAndNode(i, serverNodes.get(i));
            }
        }

        if (state.nodes.size() == 0) {
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
        List<ServerNode> serverNodes = state.nodes;
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
        if (state.nodes.size() == 0) {
            throw new DatabaseDoesNotExistException("There are no nodes in the topology at all");
        }


        AtomicInteger[] stateFailures = state.failures;
        List<ServerNode> serverNodes = state.nodes;
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
            if (state.failures[i].get() == 0 && state.nodes.get(i).getServerRole() == ServerNode.Role.MEMBER) {
                return new CurrentIndexAndNode(i, state.nodes.get(i));
            }
        }

        for (int i = 0; i < index; i++) {
            if (state.failures[i].get() == 0 && state.nodes.get(i).getServerRole() == ServerNode.Role.MEMBER) {
                return new CurrentIndexAndNode(i, state.nodes.get(i));
            }
        }

        return getPreferredNode();
    }

    public CurrentIndexAndNode getFastestNode() {
        NodeSelectorState state = _state;
        if (state.failures[state.fastest].get() == 0 && state.nodes.get(state.fastest).getServerRole() == ServerNode.Role.MEMBER) {
            return new CurrentIndexAndNode(state.fastest, state.nodes.get(state.fastest));
        }

        // if the fastest node has failures, we'll immediately schedule
        // another run of finding who the fastest node is, in the meantime
        // we'll just use the server preferred node or failover as usual

        switchToSpeedTestPhase();
        return getPreferredNode();
    }

    public void restoreNodeIndex(ServerNode node) {
        NodeSelectorState state = _state;
        int nodeIndex = state.nodes.indexOf(node);
        if (nodeIndex == -1) {
            return;
        }

        state.failures[nodeIndex].set(0);
    }

    protected static void throwEmptyTopology() {
        throw new IllegalStateException("Empty database topology, this shouldn't happen.");
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

        if (node != state.nodes.get(index)) {
            return;
        }

        if (++stateFastest[index] >= 10) {
            selectFastest(state, index);
        }

        if (state.speedTestMode.incrementAndGet() <= state.nodes.size() * 10) {
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

        ensureFastestNodeTimerExists();
        _updateFastestNodeTimer.change(Duration.ofMinutes(1), null);
    }

    public void scheduleSpeedTest() {
        ensureFastestNodeTimerExists();
        switchToSpeedTestPhase();
    }

    private void ensureFastestNodeTimerExists() {
        if (_updateFastestNodeTimer == null) {
            _updateFastestNodeTimer = new Timer(this::switchToSpeedTestPhase, null, null, executorService);
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
        public final List<ServerNode> nodes;
        public final AtomicInteger[] failures;
        public final int[] fastestRecords;
        public int fastest;
        public final AtomicInteger speedTestMode = new AtomicInteger(0);
        public int unlikelyEveryoneFaultedChoiceIndex;

        public NodeSelectorState(Topology topology) {
            this.topology = topology;
            this.nodes = topology.getNodes();
            this.failures = new AtomicInteger[topology.getNodes().size()];
            for (int i = 0; i < this.failures.length; i++) {
                this.failures[i] = new AtomicInteger(0);
            }
            this.fastestRecords = new int[topology.getNodes().size()];
            this.unlikelyEveryoneFaultedChoiceIndex = 0;
        }

        public CurrentIndexAndNode getNodeWhenEveryoneMarkedAsFaulted() {
            int index = unlikelyEveryoneFaultedChoiceIndex;
            this.unlikelyEveryoneFaultedChoiceIndex = (unlikelyEveryoneFaultedChoiceIndex + 1) % nodes.size();

            return new CurrentIndexAndNode(index, nodes.get(index));
        }
    }

}
