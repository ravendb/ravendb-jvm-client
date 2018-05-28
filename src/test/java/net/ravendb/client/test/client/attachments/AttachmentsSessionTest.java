package net.ravendb.client.test.client.attachments;

import net.ravendb.client.Constants;
import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.commands.batches.DeleteCommandData;
import net.ravendb.client.documents.operations.attachments.AttachmentName;
import net.ravendb.client.documents.operations.attachments.CloseableAttachmentResult;
import net.ravendb.client.documents.operations.attachments.DeleteAttachmentOperation;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.documents.session.IMetadataDictionary;
import net.ravendb.client.infrastructure.entities.User;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class AttachmentsSessionTest extends RemoteTestBase {

    @Test
    public void putAttachments() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {

            String[] names = { "profile.png", "background-photo.jpg", "fileNAME_#$1^%_בעברית.txt"};

            try (IDocumentSession session = store.openSession()) {
                ByteArrayInputStream profileStream = new ByteArrayInputStream(new byte[]{1, 2, 3});
                ByteArrayInputStream backgroundStream = new ByteArrayInputStream(new byte[]{10, 20, 30, 40, 50});
                ByteArrayInputStream fileStream = new ByteArrayInputStream(new byte[]{1, 2, 3, 4, 5});

                User user = new User();
                user.setName("Fitzchak");

                session.store(user, "users/1");

                session.advanced().attachments().store("users/1", names[0], profileStream, "image/png");
                session.advanced().attachments().store(user, names[1], backgroundStream, "ImGgE/jPeG");
                session.advanced().attachments().store(user, names[2], fileStream);

                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                User user = session.load(User.class, "users/1");
                IMetadataDictionary metadata = session.advanced().getMetadataFor(user);
                assertThat(metadata.get(Constants.Documents.Metadata.FLAGS).toString())
                        .isEqualTo("HasAttachments");

                Object[] attachments = (Object[]) metadata.get(Constants.Documents.Metadata.ATTACHMENTS);

                assertThat(attachments)
                        .hasSize(3);

                String[] orderedNames = Arrays.stream(names).sorted().toArray(String[]::new);

                for (int i = 0; i < names.length; i++) {
                    String name = orderedNames[i];
                    IMetadataDictionary attachment = (IMetadataDictionary) attachments[i];

                    assertThat(attachment.get("Name"))
                            .isEqualTo(name);
                }
            }
        }
    }

    @Test
    public void throwIfStreamIsUseTwice() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                ByteArrayInputStream stream = new ByteArrayInputStream(new byte[]{1, 2, 3});

                User user = new User();
                user.setName("Fitzchak");
                session.store(user, "users/1");

                session.advanced().attachments().store(user, "profile", stream, "image/png");
                session.advanced().attachments().store(user, "other", stream);

                assertThatThrownBy(() -> session.saveChanges()).isExactlyInstanceOf(IllegalStateException.class);
            }
        }
    }

    @Test
    public void throwWhenTwoAttachmentsWithTheSameNameInSession() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                ByteArrayInputStream stream = new ByteArrayInputStream(new byte[]{1, 2, 3});
                ByteArrayInputStream stream2 = new ByteArrayInputStream(new byte[]{1, 2, 3, 4, 5});
                User user = new User();
                user.setName("Fitzchak");
                session.store(user, "users/1");

                session.advanced().attachments().store(user, "profile", stream, "image/png");

                assertThatThrownBy(() -> {
                    session.advanced().attachments().store(user, "profile", stream2);
                }).isExactlyInstanceOf(IllegalStateException.class);
            }
        }
    }


    @Test
    public void putDocumentAndAttachmentAndDeleteShouldThrow() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                ByteArrayInputStream profileStream = new ByteArrayInputStream(new byte[]{1, 2, 3});

                User user = new User();
                user.setName("Fitzchak");
                session.store(user, "users/1");

                session.advanced().attachments().store(user, "profile.png", profileStream, "image/png");

                session.delete(user);

                assertThatThrownBy(() -> session.saveChanges())
                        .isExactlyInstanceOf(IllegalStateException.class);
            }
        }
    }


    @Test
    public void deleteAttachments() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                User user = new User();
                user.setName("Fitzchak");
                session.store(user, "users/1");

                ByteArrayInputStream stream1 = new ByteArrayInputStream(new byte[]{1, 2, 3});
                ByteArrayInputStream stream2 = new ByteArrayInputStream(new byte[]{1, 2, 3, 4, 5, 6});
                ByteArrayInputStream stream3 = new ByteArrayInputStream(new byte[]{1, 2, 3, 4, 5, 6, 7, 8, 9});
                ByteArrayInputStream stream4 = new ByteArrayInputStream(new byte[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12});

                session.advanced().attachments().store(user, "file1", stream1, "image/png");
                session.advanced().attachments().store(user, "file2", stream2, "image/png");
                session.advanced().attachments().store(user, "file3", stream3, "image/png");
                session.advanced().attachments().store(user, "file4", stream4, "image/png");

                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                User user = session.load(User.class, "users/1");

                // test get attachment by its name
                try (CloseableAttachmentResult attachmentResult = session.advanced().attachments().get("users/1", "file2")) {
                    assertThat(attachmentResult.getDetails().getName())
                            .isEqualTo("file2");
                }

                session.advanced().attachments().delete("users/1", "file2");
                session.advanced().attachments().delete(user, "file4");
                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                User user = session.load(User.class, "users/1");
                IMetadataDictionary metadata = session.advanced().getMetadataFor(user);
                assertThat(metadata.get(Constants.Documents.Metadata.FLAGS).toString())
                        .isEqualTo("HasAttachments");

                Object[] attachments = (Object[]) metadata.get(Constants.Documents.Metadata.ATTACHMENTS);

                assertThat(attachments)
                        .hasSize(2);

                CloseableAttachmentResult result = session.advanced().attachments().get("users/1", "file1");
                byte[] file1Bytes = IOUtils.toByteArray(result.getData());
                assertThat(file1Bytes)
                        .hasSize(3);

            }
        }
    }

    @Test
    public void deleteAttachmentsUsingCommand() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                User user = new User();
                user.setName("Fitzchak");
                session.store(user, "users/1");

                ByteArrayInputStream stream1 = new ByteArrayInputStream(new byte[]{1, 2, 3});
                ByteArrayInputStream stream2 = new ByteArrayInputStream(new byte[]{1, 2, 3, 4, 5, 6});

                session.advanced().attachments().store(user, "file1", stream1, "image/png");
                session.advanced().attachments().store(user, "file2", stream2, "image/png");

                session.saveChanges();
            }

            store.operations().send(new DeleteAttachmentOperation("users/1", "file2"));

            try (IDocumentSession session = store.openSession()) {
                User user = session.load(User.class, "users/1");
                IMetadataDictionary metadata = session.advanced().getMetadataFor(user);
                assertThat(metadata.get(Constants.Documents.Metadata.FLAGS).toString())
                        .isEqualTo("HasAttachments");

                Object[] attachments = (Object[]) metadata.get(Constants.Documents.Metadata.ATTACHMENTS);

                assertThat(attachments)
                        .hasSize(1);

                CloseableAttachmentResult result = session.advanced().attachments().get("users/1", "file1");
                byte[] file1Bytes = IOUtils.toByteArray(result.getData());
                assertThat(file1Bytes)
                        .hasSize(3);

            }
        }
    }

    @Test
    public void getAttachmentReleasesResources() throws Exception {
        int count = 30;

        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                User user = new User();
                session.store(user, "users/1");
                session.saveChanges();
            }

            for (int i = 0; i < count; i++) {
                try (IDocumentSession session = store.openSession()) {
                    ByteArrayInputStream stream1 = new ByteArrayInputStream(new byte[]{1, 2, 3});
                    session.advanced().attachments().store("users/1", "file" + i, stream1, "image/png");
                    session.saveChanges();
                }
            }

            for (int i = 0; i < count; i++) {
                try (IDocumentSession session = store.openSession()) {
                    try (CloseableAttachmentResult result = session.advanced().attachments().get("users/1", "file" + i))
                    {
                        // don't read data as it marks entity as consumed
                    }
                }
            }
        }
    }

    @Test
    public void deleteDocumentAndThanItsAttachments_ThisIsNoOpButShouldBeSupported() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                User user = new User();
                user.setName("Fitzchak");
                session.store(user, "users/1");

                ByteArrayInputStream stream = new ByteArrayInputStream(new byte[]{1, 2, 3});

                session.advanced().attachments().store(user, "file", stream, "image/png");
                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                User user = session.load(User.class, "users/1");

                session.delete(user);
                session.advanced().attachments().delete(user, "file");
                session.advanced().attachments().delete(user, "file"); // this should be no-op

                session.saveChanges();
            }
        }
    }

    @Test
    public void deleteDocumentByCommandAndThanItsAttachments_ThisIsNoOpButShouldBeSupported() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                User user = new User();
                user.setName("Fitzchak");
                session.store(user, "users/1");

                ByteArrayInputStream stream = new ByteArrayInputStream(new byte[]{1, 2, 3});

                session.advanced().attachments().store(user, "file", stream, "image/png");
                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                session.advanced().defer(new DeleteCommandData("users/1", null));
                session.advanced().attachments().delete("users/1", "file");
                session.advanced().attachments().delete("users/1", "file");

                session.saveChanges();
            }
        }
    }

    @Test
    public void getAttachmentNames() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            String[] names = {"profile.png"};

            try (IDocumentSession session = store.openSession()) {
                ByteArrayInputStream profileStream = new ByteArrayInputStream(new byte[]{1, 2, 3});

                User user = new User();
                user.setName("Fitzchak");
                session.store(user, "users/1");

                session.advanced().attachments().store("users/1", names[0], profileStream, "image/png");

                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                User user = session.load(User.class, "users/1");
                AttachmentName[] attachments = session.advanced().attachments().getNames(user);

                assertThat(attachments)
                        .hasSize(1);

                AttachmentName attachment = attachments[0];

                assertThat(attachment.getContentType())
                        .isEqualTo("image/png");

                assertThat(attachment.getName())
                        .isEqualTo(names[0]);

                assertThat(attachment.getSize())
                        .isEqualTo(3);
            }
        }
    }

    @Test
    public void attachmentExists() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                ByteArrayInputStream stream = new ByteArrayInputStream(new byte[]{1, 2, 3});

                User user = new User();
                user.setName("Fitzchak");

                session.store(user, "users/1");

                session.advanced().attachments().store("users/1", "profile", stream, "image/png");
                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                assertThat(session.advanced().attachments().exists("users/1", "profile"))
                        .isTrue();

                assertThat(session.advanced().attachments().exists("users/1", "background-photo"))
                        .isFalse();

                assertThat(session.advanced().attachments().exists("users/2", "profile"))
                        .isFalse();
            }
        }
    }
}
