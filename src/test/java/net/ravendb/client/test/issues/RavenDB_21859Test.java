package net.ravendb.client.test.issues;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.session.IDocumentSession;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class RavenDB_21859Test extends RemoteTestBase {

    @Test
    public void load_And_Lazy_Load_Should_Return_Null_When_Id_Is_Null() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                assertThat(session.load(Object.class, (String) null)).isNull();

                assertThat(session.advanced().lazily().load(Object.class, (String) null).getValue()).isNull();

                assertThat(session.advanced().getNumberOfRequests()).isZero();
            }
        }
    }
}
