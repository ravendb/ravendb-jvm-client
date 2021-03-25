package net.ravendb.client.serverwide.operations;

import net.ravendb.client.documents.DocumentStore;
import net.ravendb.client.http.*;
import net.ravendb.client.primitives.CleanCloseable;

import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.stream.Collectors;

public class ServerOperationExecutor implements CleanCloseable {

    private final ConcurrentMap<String, ServerOperationExecutor> _cache;
    private final String _nodeTag;
    private final DocumentStore _store;
    private final ClusterRequestExecutor _requestExecutor;
    private final ClusterRequestExecutor _initialRequestExecutor;

    public ServerOperationExecutor(DocumentStore store) {
        this(store, createRequestExecutor(store), null, new ConcurrentSkipListMap<>(String::compareToIgnoreCase), null);
    }

    private ServerOperationExecutor(DocumentStore store, ClusterRequestExecutor requestExecutor, ClusterRequestExecutor initialRequestExecutor,
                                    ConcurrentMap<String, ServerOperationExecutor> cache, String nodeTag) {
        if (store == null) {
            throw new IllegalArgumentException("Store cannot be null");
        }

        if (requestExecutor == null) {
            throw new IllegalArgumentException("RequestExecutor cannot be null");
        }

        _store = store;
        _requestExecutor = requestExecutor;
        _initialRequestExecutor = initialRequestExecutor;
        _nodeTag = nodeTag;
        _cache = cache;

        store.registerEvents(_requestExecutor);

        if (_nodeTag == null) {
            store.addAfterCloseListener((sender, event) -> _requestExecutor.close());
        }
    }



    public void send(IVoidServerOperation operation) {
        VoidRavenCommand command = operation.getCommand(_requestExecutor.getConventions());
        _requestExecutor.execute(command);
    }

    @SuppressWarnings("UnusedReturnValue")
    public <TResult> TResult send(IServerOperation<TResult> operation) {
        RavenCommand<TResult> command = operation.getCommand(_requestExecutor.getConventions());
        _requestExecutor.execute(command);

        return command.getResult();
    }


    @Override
    public void close() {
        if (_nodeTag != null) {
            return;
        }

        if (_requestExecutor != null) {
            _requestExecutor.close();
        }

        ConcurrentMap<String, ServerOperationExecutor> cache = _cache;
        if (cache != null) {
            for (Map.Entry<String, ServerOperationExecutor> kvp : cache.entrySet()) {
                ClusterRequestExecutor requestExecutor = kvp.getValue()._requestExecutor;
                if (requestExecutor != null) {
                    requestExecutor.close();
                }
            }

            cache.clear();
        }
    }


    private static ClusterRequestExecutor createRequestExecutor(DocumentStore store) {
        return store.getConventions().isDisableTopologyUpdates() ?
                ClusterRequestExecutor.createForSingleNode(store.getUrls()[0], store.getCertificate(), store.getCertificatePrivateKeyPassword(), store.getTrustStore(), store.getExecutorService(), store.getConventions()) :
                ClusterRequestExecutor.create(store.getUrls(), store.getCertificate(), store.getCertificatePrivateKeyPassword(), store.getTrustStore(), store.getExecutorService(), store.getConventions());
    }

}
