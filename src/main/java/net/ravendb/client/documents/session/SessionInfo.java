package net.ravendb.client.documents.session;

import net.ravendb.client.http.CurrentIndexAndNode;
import net.ravendb.client.http.LoadBalanceBehavior;
import net.ravendb.client.http.RequestExecutor;
import net.ravendb.client.http.ServerNode;
import org.apache.commons.codec.digest.DigestUtils;

import java.nio.ByteBuffer;

public class SessionInfo {

    private static final ThreadLocal<Integer> _clientSessionIdCounter = ThreadLocal.withInitial(() -> 0);

    private int _clientSessionId;
    private boolean _sessionIdUsed;
    private final int _loadBalancerContextSeed;

    private Long lastClusterTransactionIndex;
    private boolean noCaching;

    public SessionInfo(String sessionKey, int loadBalancerContextSeed) {
        this(sessionKey, loadBalancerContextSeed, null, false);
    }

    public SessionInfo(String sessionKey, int loadBalancerContextSeed, Long lastClusterTransactionIndex, boolean noCaching) {
        setContext(sessionKey, loadBalancerContextSeed);

        _loadBalancerContextSeed = loadBalancerContextSeed;

        this.lastClusterTransactionIndex = lastClusterTransactionIndex;
        this.noCaching = noCaching;
    }

    public void setContext(String sessionKey) {
        setContext(sessionKey, _loadBalancerContextSeed);
    }

    private void setContext(String sessionKey, int loadBalancerContextSeed) {
        if (_sessionIdUsed) {
            throw new IllegalStateException("Unable to set the session context after it has already been used. The session context can only be modified before it is utilized.");
        }

        if (sessionKey == null) {
            Integer v = _clientSessionIdCounter.get();
            _clientSessionId = ++v;
            _clientSessionIdCounter.set(v);
        } else {

            byte[] sessionKeyBytes = sessionKey.getBytes();
            byte[] bytesToHash = ByteBuffer
                    .allocate(sessionKeyBytes.length + 4)
                    .put(sessionKeyBytes)
                    .putInt(loadBalancerContextSeed)
                    .array();
            byte[] md5Bytes = DigestUtils.md5(bytesToHash);
            _clientSessionId = ByteBuffer.wrap(md5Bytes)
                    .getInt();
        }
    }

    public ServerNode getCurrentSessionNode(RequestExecutor requestExecutor) {
        CurrentIndexAndNode result;

        if (requestExecutor.getConventions().getLoadBalanceBehavior() == LoadBalanceBehavior.USE_SESSION_CONTEXT) {
            result = requestExecutor.getNodeBySessionId(_clientSessionId);
            return result.currentNode;
        }

        switch (requestExecutor.getConventions().getReadBalanceBehavior()) {
            case NONE:
                result = requestExecutor.getPreferredNode();
                break;
            case ROUND_ROBIN:
                result = requestExecutor.getNodeBySessionId(_clientSessionId);
                break;
            case FASTEST_NODE:
                result = requestExecutor.getFastestNode();
                break;
            default:
                throw new IllegalArgumentException(requestExecutor.getConventions().getReadBalanceBehavior().toString());
        }

        return result.currentNode;
    }

    public Integer getSessionId() {
        _sessionIdUsed = true;
        return _clientSessionId;
    }

    public Long getLastClusterTransactionIndex() {
        return lastClusterTransactionIndex;
    }

    public void setLastClusterTransactionIndex(Long lastClusterTransactionIndex) {
        this.lastClusterTransactionIndex = lastClusterTransactionIndex;
    }

    public boolean isNoCaching() {
        return noCaching;
    }

    public void setNoCaching(boolean noCaching) {
        this.noCaching = noCaching;
    }

}
