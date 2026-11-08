package net.ravendb.client.test.issues;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.session.DocumentsChanges;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.infrastructure.entities.User;
import net.ravendb.client.primitives.NetISO8601Utils;
import org.apache.commons.lang3.time.DateUtils;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class RavenDB_25024Test extends RemoteTestBase {

    @Test
    public void changeMetadataDoesNotShowChanges() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            String id = "users/1";
            try (IDocumentSession session = store.openSession()) {
                User user = new User();
                user.setName("user1");
                session.store(user, id);
                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                User user = session.load(User.class, id);
                session.advanced().getMetadataFor(user).put("@refresh", NetISO8601Utils.format(DateUtils.addDays(new Date(), 1), true));

                Map<String, List<DocumentsChanges>> whatChanged = session.advanced().whatChanged();
                boolean sessionHasChanges = session.advanced().hasChanges();
                boolean entityHasChanges = session.advanced().hasChanged(user);

                assertThat(whatChanged).isNotEmpty();
                assertThat(entityHasChanges).isTrue();
                assertThat(sessionHasChanges).isTrue();
            }

            // hasChanged / hasChanges must detect metadata-only edits on their own (without a prior whatChanged call)
            try (IDocumentSession session = store.openSession()) {
                User user = session.load(User.class, id);
                session.advanced().getMetadataFor(user).put("@refresh", NetISO8601Utils.format(DateUtils.addDays(new Date(), 1), true));

                assertThat(session.advanced().hasChanged(user)).isTrue();
            }

            try (IDocumentSession session = store.openSession()) {
                User user = session.load(User.class, id);
                session.advanced().getMetadataFor(user).put("@refresh", NetISO8601Utils.format(DateUtils.addDays(new Date(), 1), true));

                assertThat(session.advanced().hasChanges()).isTrue();
            }
        }
    }
}
