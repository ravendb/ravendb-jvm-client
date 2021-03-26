package net.ravendb.client.documents;

import net.ravendb.client.documents.identity.MultiDatabaseHiLoIdGenerator;
import net.ravendb.client.documents.operations.MaintenanceOperationExecutor;
import net.ravendb.client.documents.operations.OperationExecutor;
import net.ravendb.client.documents.session.DocumentSession;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.documents.session.SessionOptions;
import net.ravendb.client.http.RequestExecutor;
import net.ravendb.client.primitives.*;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.function.Supplier;

/**
 * Manages access to RavenDB and open sessions to work with RavenDB.
 */
public class DocumentStore extends DocumentStoreBase {

    private ExecutorService executorService = Executors.newCachedThreadPool();

    private final ConcurrentMap<String, Lazy<RequestExecutor>> requestExecutors = new ConcurrentSkipListMap<>(String.CASE_INSENSITIVE_ORDER);

    private MultiDatabaseHiLoIdGenerator _multiDbHiLo;

    private MaintenanceOperationExecutor maintenanceOperationExecutor;
    private OperationExecutor operationExecutor;

    private String identifier;

    public DocumentStore(String url, String database) {
        this.setUrls(new String[]{url});
        this.setDatabase(database);
    }

    public DocumentStore(String[] urls, String database) {
        this.setUrls(urls);
        this.setDatabase(database);
    }

    public DocumentStore() {

    }

    public ExecutorService getExecutorService() {
        return executorService;
    }

    /**
     * Gets the identifier for this store.
     */
    public String getIdentifier() {
        if (identifier != null) {
            return identifier;
        }

        if (urls == null) {
            return null;
        }

        if (database != null) {
            return String.join(",", urls) + " (DB: " + database + ")";
        }

        return String.join(",", urls);
    }

    /**
     * Sets the identifier for this store.
     */
    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    @SuppressWarnings("EmptyTryBlock")
    public void close() {
        EventHelper.invoke(beforeClose, this, EventArgs.EMPTY);

        if (_multiDbHiLo != null) {
            try {
                _multiDbHiLo.returnUnusedRange();
            } catch (Exception e) {
                // ignore
            }
        }

        disposed = true;

        EventHelper.invoke(new ArrayList<>(afterClose), this, EventArgs.EMPTY);

        for (Map.Entry<String, Lazy<RequestExecutor>> kvp : requestExecutors.entrySet()) {
            if (!kvp.getValue().isValueCreated()) {
                continue;
            }

            kvp.getValue().getValue().close();
        }

        executorService.shutdown();
    }

    /**
     * Opens the session.
     */
    @Override
    public IDocumentSession openSession() {
        return openSession(new SessionOptions());
    }

    /**
     * Opens the session for a particular database
     */
    @Override
    public IDocumentSession openSession(String database) {
        SessionOptions sessionOptions = new SessionOptions();
        sessionOptions.setDatabase(database);

        return openSession(sessionOptions);
    }

    @Override
    public IDocumentSession openSession(SessionOptions options) {
        assertInitialized();
        ensureNotClosed();

        UUID sessionId = UUID.randomUUID();
        DocumentSession session = new DocumentSession(this, sessionId, options);
        registerEvents(session);
        afterSessionCreated(session);
        return session;
    }

    @Override
    public RequestExecutor getRequestExecutor() {
        return getRequestExecutor(null);
    }

    @Override
    public RequestExecutor getRequestExecutor(String database) {
        assertInitialized();

        database = getEffectiveDatabase(database);

        Lazy<RequestExecutor> executor = requestExecutors.get(database);
        if (executor != null) {
            return executor.getValue();
        }

        final String effectiveDatabase = database;

        Supplier<RequestExecutor> createRequestExecutor = () -> {
            RequestExecutor requestExecutor = RequestExecutor.create(getUrls(), effectiveDatabase, executorService, getConventions());
            registerEvents(requestExecutor);

            return requestExecutor;
        };

        Supplier<RequestExecutor> createRequestExecutorForSingleNode = () -> {
            RequestExecutor forSingleNode = RequestExecutor.createForSingleNodeWithConfigurationUpdates(getUrls()[0], effectiveDatabase, executorService, getConventions());
            registerEvents(forSingleNode);

            return forSingleNode;
        };

        if (!getConventions().isDisableTopologyUpdates()) {
            executor = new Lazy<>(createRequestExecutor);
        } else {
            executor = new Lazy<>(createRequestExecutorForSingleNode);
        }

        requestExecutors.put(database, executor);

        return executor.getValue();
    }


    /**
     * Initializes this instance.
     */
    @Override
    public IDocumentStore initialize() {
        if (initialized) {
            return this;
        }

        assertValidConfiguration();

        RequestExecutor.validateUrls(urls);

        try {
            if (getConventions().getDocumentIdGenerator() == null) { // don't overwrite what the user is doing
                MultiDatabaseHiLoIdGenerator generator = new MultiDatabaseHiLoIdGenerator(this);
                _multiDbHiLo = generator;

                getConventions().setDocumentIdGenerator(generator::generateDocumentId);
            }

            getConventions().freeze();
            initialized = true;
        } catch (Exception e) {
            close();
            throw ExceptionsUtils.unwrapException(e);
        }

        return this;
    }


    /**
     * Validate the configuration for the document store
     */
    protected void assertValidConfiguration() {
        if (urls == null || urls.length == 0) {
            throw new IllegalArgumentException("Document store URLs cannot be empty");
        }
    }





    private final List<EventHandler<VoidArgs>> afterClose = new ArrayList<>();

    private final List<EventHandler<VoidArgs>> beforeClose = new ArrayList<>();

    public void addBeforeCloseListener(EventHandler<VoidArgs> event) {
        this.beforeClose.add(event);
    }

    @Override
    public void removeBeforeCloseListener(EventHandler<VoidArgs> event) {
        this.beforeClose.remove(event);
    }

    public void addAfterCloseListener(EventHandler<VoidArgs> event) {
        this.afterClose.add(event);
    }

    @Override
    public void removeAfterCloseListener(EventHandler<VoidArgs> event) {
        this.afterClose.remove(event);
    }


    @Override
    public MaintenanceOperationExecutor maintenance() {
        assertInitialized();

        if (maintenanceOperationExecutor == null) {
            maintenanceOperationExecutor = new MaintenanceOperationExecutor(this);
        }

        return maintenanceOperationExecutor;
    }

    @Override
    public OperationExecutor operations() {
        if (operationExecutor == null) {
            operationExecutor = new OperationExecutor(this);
        }

        return operationExecutor;
    }


}
