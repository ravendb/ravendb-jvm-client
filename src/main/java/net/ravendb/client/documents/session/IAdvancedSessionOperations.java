package net.ravendb.client.documents.session;

public interface IAdvancedSessionOperations extends IAdvancedDocumentSessionOperations, IDocumentQueryBuilder {



    /**
     * Check if document exists
     * @param id document id to check
     * @return true if document exists
     */
    boolean exists(String id);


}
