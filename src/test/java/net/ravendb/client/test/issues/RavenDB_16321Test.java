package net.ravendb.client.test.issues;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.CloseableIterator;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.commands.StreamResult;
import net.ravendb.client.documents.queries.Query;
import net.ravendb.client.documents.session.IDocumentQuery;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.exceptions.documents.indexes.IndexDoesNotExistException;
import net.ravendb.client.infrastructure.orders.Employee;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class RavenDB_16321Test extends RemoteTestBase {

    @Test
    public void streamingOnIndexThatDoesNotExistShouldThrow() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                IDocumentQuery<Employee> query = session.query(Employee.class, Query.index("Does_Not_Exist"))
                        .whereEquals("firstName", "Robert");

                assertThatThrownBy(() -> {
                    try (CloseableIterator<StreamResult<Employee>> stream = session.advanced().stream(query)) {
                        if (stream.hasNext()) {
                            stream.next();
                        }
                    }
                })
                        .isExactlyInstanceOf(IndexDoesNotExistException.class);
            }
        }
    }
}
