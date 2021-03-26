package net.ravendb.client.documents;

import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.operations.MaintenanceOperationExecutor;
import net.ravendb.client.documents.operations.OperationExecutor;
import net.ravendb.client.documents.session.*;
import net.ravendb.client.http.RequestExecutor;
import net.ravendb.client.util.IDisposalNotification;

/**
 * Interface for managing access to RavenDB and open sessions.
 */
public interface IDocumentStore extends IDisposalNotification {




    /**
     * @return Gets the identifier for this store.
     */
    String getIdentifier();

    /**
     * Sets the identifier for this store.
     * @param identifier Identifier to set
     */
    void setIdentifier(String identifier);

    /**
     * Initializes this instance.
     * @return initialized store
     */
    IDocumentStore initialize();


    /**
     * Opens the session
     * @return Document session
     */
    IDocumentSession openSession();

    /**
     * Opens the session for a particular database
     * @param database Database to use
     * @return Document session
     */
    IDocumentSession openSession(String database);

    /**
     * Opens the session with the specified options.
     * @param sessionOptions Session options to use
     * @return Document session
     */
    IDocumentSession openSession(SessionOptions sessionOptions);


    /**
     * Gets the conventions
     * @return Document conventions
     */
    DocumentConventions getConventions();

    /**
     * Gets the URL's
     * @return Store urls
     */
    String[] getUrls();

    String getDatabase();

    RequestExecutor getRequestExecutor();

    RequestExecutor getRequestExecutor(String databaseName);

    MaintenanceOperationExecutor maintenance();

    OperationExecutor operations();

}
