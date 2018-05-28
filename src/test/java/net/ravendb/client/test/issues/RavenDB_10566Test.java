package net.ravendb.client.test.issues;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.documents.session.IMetadataDictionary;
import net.ravendb.client.infrastructure.entities.User;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

public class RavenDB_10566Test extends RemoteTestBase {

    @Test
    public void shouldBeAvailable() throws Exception {a
        try (IDocumentStore store = getDocumentStore()) {

            AtomicReference<String> name = new AtomicReference<>();
            store.addAfterSaveChangesListener(((sender, event) -> name.set((String) event.getDocumentMetadata().get("Name"))));


            try (IDocumentSession session = store.openSession()) {
                User user = new User();
                user.setName("Oren");

                session.store(user, "users/oren");
                IMetadataDictionary metadata = session.advanced().getMetadataFor(user);
                metadata.put("Name", "FooBar");

                session.saveChanges();
            }

            assertThat(name.get())
                    .isEqualTo("FooBar");
        }
    }

}
