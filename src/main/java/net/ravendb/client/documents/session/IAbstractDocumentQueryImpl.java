package net.ravendb.client.documents.session;

import net.ravendb.client.documents.session.operations.QueryOperation;
import net.ravendb.client.documents.session.tokens.FieldsToFetchToken;

/**
 * This is used as an abstraction for the implementation
 * of a query when passing to other parts of the
 * query infrastructure. Meant to be internal only, making
 * this public to allow mocking / instrumentation.
 */
public interface IAbstractDocumentQueryImpl<T> {
    FieldsToFetchToken getFieldsToFetchToken();

    void setFieldsToFetchToken(FieldsToFetchToken fieldsToFetchToken);

    boolean isProjectInto();

    QueryOperation initializeQueryOperation();
}
