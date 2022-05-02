package net.ravendb.client.documents.session;

import net.ravendb.client.documents.DocumentStoreBase;
import net.ravendb.client.http.CurrentIndexAndNode;
import net.ravendb.client.http.LoadBalanceBehavior;
import net.ravendb.client.http.RequestExecutor;
import net.ravendb.client.http.ServerNode;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;

import java.nio.ByteBuffer;
import java.util.function.Function;

public class SessionInfo {

    private static final ThreadLocal<Integer> _clientSessionIdCounter = ThreadLocal.withInitial(() -> 0);

    private Integer _sessionId;
    private boolean _sessionIdUsed;
    private final int _loadBalancerContextSeed;
    private boolean _canUseLoadBalanceBehavior;
    private final InMemoryDocumentSessionOperations _session;

    private Long lastClusterTransactionIndex;
    private boolean noCaching;

    public SessionInfo(InMemoryDocumentSessionOperations session, SessionOptions options, DocumentStoreBase documentStore) {
        if (documentStore == null) {
            throw new IllegalArgumentException("DocumentStore cannot be null");
        }

        if (session == null) {
            throw new IllegalArgumentException("Session cannot be null");
        }

        this._session = session;
        _loadBalancerContextSeed = session._requestExecutor.getConventions().getLoadBalancerContextSeed();
        _canUseLoadBalanceBehavior = session.getConventions().getLoadBalanceBehavior() == LoadBalanceBehavior.USE_SESSION_CONTEXT
                && session.getConventions().getLoadBalancerPerSessionContextSelector() != null;

        setLastClusterTransactionIndex(documentStore.getLastTransactionIndex(session.getDatabaseName()));
        this.noCaching = options.isNoCaching();
    }

    public void incrementRequestCount() {
        _session.incrementRequestCount();
    }

    public void setContext(String sessionKey) {
        if (StringUtils.isBlank(sessionKey)) {
            throw new IllegalArgumentException("Session key cannot be null or whitespace.");
        }

        setContextInternal(sessionKey);

        _canUseLoadBalanceBehavior = _canUseLoadBalanceBehavior || _session.getConventions().getLoadBalanceBehavior() == LoadBalanceBehavior.USE_SESSION_CONTEXT;
    }

    private void setContextInternal(String sessionKey) {
        if (_sessionIdUsed) {
            throw new IllegalStateException("Unable to set the session context after it has already been used. The session context can only be modified before it is utilized.");
        }

        if (sessionKey == null) {
            Integer v = _clientSessionIdCounter.get();
            _sessionId = ++v;
            _clientSessionIdCounter.set(v);
        } else {

            byte[] sessionKeyBytes = sessionKey.getBytes();
            byte[] bytesToHash = ByteBuffer
                    .allocate(sessionKeyBytes.length + 4)
                    .put(sessionKeyBytes)
                    .putInt(_loadBalancerContextSeed)
                    .array();
            byte[] md5Bytes = DigestUtils.md5(bytesToHash);
            _sessionId = ByteBuffer.wrap(md5Bytes)
                    .getInt();
        }
    }

    public ServerNode getCurrentSessionNode(RequestExecutor requestExecutor) {
        CurrentIndexAndNode result;

        if (requestExecutor.getConventions().getLoadBalanceBehavior() == LoadBalanceBehavior.USE_SESSION_CONTEXT) {
            if (_canUseLoadBalanceBehavior) {
                result = requestExecutor.getNodeBySessionId(getSessionId());
                return result.currentNode;
            }
        }

        switch (requestExecutor.getConventions().getReadBalanceBehavior()) {
            case NONE:
                result = requestExecutor.getPreferredNode();
                break;
            case ROUND_ROBIN:
                result = requestExecutor.getNodeBySessionId(getSessionId());
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
        if (_sessionId == null) {
            String context = null;
            Function<String, String> selector =
                    _session.getConventions().getLoadBalancerPerSessionContextSelector();
            if (selector != null) {
                context = selector.apply(_session.getDatabaseName());
            }
            setContextInternal(context);
        }
        _sessionIdUsed = true;
        return _sessionId;
    }

    public boolean canUseLoadBalanceBehavior() {
        return this._canUseLoadBalanceBehavior;
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
