package net.ravendb.client;

import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.indexes.IndexErrors;
import net.ravendb.client.documents.indexes.IndexingError;
import net.ravendb.client.documents.operations.indexes.GetIndexErrorsOperation;

public class RavenTestHelper {

    public static void assertNoIndexErrors(IDocumentStore store) {
        assertNoIndexErrors(store, null);
    }

    public static void assertNoIndexErrors(IDocumentStore store, String databaseName) {
        IndexErrors[] errors = store.maintenance().forDatabase(databaseName).send(new GetIndexErrorsOperation());

        StringBuilder sb = null;
        for (IndexErrors indexErrors : errors) {
            if (indexErrors == null || indexErrors.getErrors() == null || indexErrors.getErrors().length == 0) {
                continue;
            }

            if (sb == null) {
                sb = new StringBuilder();
            }

            sb.append("Index Errors for '")
                    .append(indexErrors.getName())
                    .append("' (")
                    .append(indexErrors.getErrors().length)
                    .append(")");
            sb.append(System.lineSeparator());

            for (IndexingError indexError : indexErrors.getErrors()) {
                sb.append("- " + indexError);
                sb.append(System.lineSeparator());
            }

            sb.append(System.lineSeparator());
        }

        if (sb == null) {
            return;
        }

        throw new IllegalStateException(sb.toString());
    }
}
