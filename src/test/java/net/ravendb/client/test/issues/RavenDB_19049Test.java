package net.ravendb.client.test.issues;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.operations.attachments.CloseableAttachmentResult;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.exceptions.documents.attachments.InvalidAttachmentRangeException;
import net.ravendb.client.infrastructure.EnableOnServer;
import net.ravendb.client.infrastructure.entities.User;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@EnableOnServer(thresholdVersion = "6.2")
public class RavenDB_19049Test extends RemoteTestBase {

    private static final String ID = "users/1";
    private static final String NAME = "file.txt";

    private static void setup(IDocumentStore store) {
        try (IDocumentSession session = store.openSession()) {
            User user = new User();
            user.setName("Fitzchak");
            session.store(user, ID);

            session.advanced().attachments().store(user, NAME, new ByteArrayInputStream(new byte[]{1, 2, 3, 4, 5, 6, 7, 8, 9}));

            session.saveChanges();
        }
    }

    @Test
    public void returnsRange() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            setup(store);

            try (IDocumentSession session = store.openSession();
                 CloseableAttachmentResult attachment = session.advanced().attachments().getRange(ID, NAME, 3L, 5L)) {
                byte[] bytes = IOUtils.toByteArray(attachment.getData());

                assertThat(attachment.getDetails().getSize()).isEqualTo(9);
                assertThat(bytes).containsExactly(4, 5, 6);
            }

            try (IDocumentSession session = store.openSession()) {
                User user = session.load(User.class, ID);
                try (CloseableAttachmentResult attachment = session.advanced().attachments().getRange(user, NAME, 7L, null)) {
                    assertThat(IOUtils.toByteArray(attachment.getData())).containsExactly(8, 9);
                }
            }
        }
    }

    @Test
    public void throwsForUnsupportedRange() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            setup(store);

            try (IDocumentSession session = store.openSession()) {
                assertThatThrownBy(() -> session.advanced().attachments().getRange(ID, NAME, 999L, 1000L))
                        .isInstanceOf(InvalidAttachmentRangeException.class)
                        .hasMessage("There requested range '999-1000' is invalid for 'file.txt' in document 'users/1'.");

                assertThatThrownBy(() -> session.advanced().attachments().getRange(ID, NAME, -5L, 5L))
                        .isInstanceOf(IllegalArgumentException.class);

                assertThatThrownBy(() -> session.advanced().attachments().getRange(ID, NAME, 10L, 5L))
                        .isInstanceOf(IllegalArgumentException.class);
            }
        }
    }
}
