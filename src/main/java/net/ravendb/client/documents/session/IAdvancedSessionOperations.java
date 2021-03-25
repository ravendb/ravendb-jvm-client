package net.ravendb.client.documents.session;

public interface IAdvancedSessionOperations extends IAdvancedDocumentSessionOperations, IDocumentQueryBuilder {


    /**
     * Updates entity with latest changes from server
     * @param <T> entity class
     * @param entity Entity to refresh
     */
    <T> void refresh(T entity);


    /**
     * Check if document exists
     * @param id document id to check
     * @return true if document exists
     */
    boolean exists(String id);


}
