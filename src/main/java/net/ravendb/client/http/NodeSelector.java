package net.ravendb.client.http;

import net.ravendb.client.exceptions.AllTopologyNodesDownException;
import net.ravendb.client.primitives.CleanCloseable;
import net.ravendb.client.primitives.Timer;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

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

    public CurrentIndexAndNode getPreferredNode() {
        NodeSelectorState state = _state;
        AtomicInteger[] stateFailures = state.failures;
        List<ServerNode> serverNodes = state.nodes;
        int len = Math.min(serverNodes.size(), stateFailures.length);
        for (int i = 0; i < len; i++) {
            if (stateFailures[i].get() == 0 && StringUtils.isNotEmpty(serverNodes.get(i).getUrl())) {
                return new CurrentIndexAndNode(i, serverNodes.get(i));
            }
        }
        return unlikelyEveryoneFaultedChoice(state);
    }

    private static CurrentIndexAndNode unlikelyEveryoneFaultedChoice(NodeSelectorState state) {
        // if there are all marked as failed, we'll chose the first
        // one so the user will get an error (or recover :-) );
        if (state.nodes.size() == 0) {
            throw new AllTopologyNodesDownException("There are no nodes in the topology at all");
        }

        return new CurrentIndexAndNode (0, state.nodes.get(0));
    }

    public CurrentIndexAndNode getNodeBySessionId(int sessionId) {
        NodeSelectorState state = _state;
        int index = sessionId % state.topology.getNodes().size();

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

    public void restoreNodeIndex(int nodeIndex) {
        NodeSelectorState state = _state;
        if (state.failures.length < nodeIndex) {
            return; // the state was changed and we no longer have it?
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

        if (_updateFastestNodeTimer != null) {
            _updateFastestNodeTimer.change(Duration.ofMinutes(1));
        } else {
            _updateFastestNodeTimer = new Timer(this::switchToSpeedTestPhase, Duration.ofMinutes(1), executorService);
        }
    }

    public void scheduleSpeedTest() {
        switchToSpeedTestPhase();
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

        public NodeSelectorState(Topology topology) {
            this.topology = topology;
            this.nodes = topology.getNodes();
            this.failures = new AtomicInteger[topology.getNodes().size()];
            for (int i = 0; i < this.failures.length; i++) {
                this.failures[i] = new AtomicInteger(0);
            }
            this.fastestRecords = new int[topology.getNodes().size()];

        }
    }

}
