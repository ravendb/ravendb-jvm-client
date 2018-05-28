package net.ravendb.client.test.client.attachments;

import net.ravendb.client.Constants;
import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.DocumentStore;
import net.ravendb.client.documents.commands.DeleteDocumentCommand;
import net.ravendb.client.documents.operations.DatabaseStatistics;
import net.ravendb.client.documents.operations.GetStatisticsOperation;
import net.ravendb.client.documents.operations.attachments.AttachmentDetails;
import net.ravendb.client.documents.operations.attachments.CloseableAttachmentResult;
import net.ravendb.client.documents.operations.attachments.PutAttachmentOperation;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.documents.session.IMetadataDictionary;
import net.ravendb.client.infrastructure.entities.User;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

public class AttachmentsRevisionsTest extends RemoteTestBase {

    @Test
    public void putAttachments() throws Exception {
        try (DocumentStore store = getDocumentStore()) {
            setupRevisions(store, false, 4);

            String[] names = createDocumentWithAttachments(store);

            assertRevisions(store, names, (session, revisions) -> {
                assertRevisionAttachments(names, 3, revisions.get(0), session);
                assertRevisionAttachments(names, 2, revisions.get(1), session);
                assertRevisionAttachments(names, 1, revisions.get(2), session);
                assertNoRevisionAttachment(revisions.get(3), session, false);
            }, 9);

            // Delete document should delete all the attachments
            store.getRequestExecutor().execute(new DeleteDocumentCommand("users/1"));
            assertRevisions(store, names, (session, revisions) -> {
                assertNoRevisionAttachment(revisions.get(0), session, true);
                assertRevisionAttachments(names, 3, revisions.get(1), session);
                assertRevisionAttachments(names, 2, revisions.get(2), session);
                assertRevisionAttachments(names, 1, revisions.get(3), session);

            }, 6, 0, 3);

            // Create another revision which should delete old revision
            try (IDocumentSession session = store.openSession()) { // This will delete the revision #1 which is without attachment
                User user = new User();
                user.setName("Fitzchak 2");
                session.store(user, "users/1");
                session.saveChanges();
            }

            assertRevisions(store, names, (session, revisions) -> { // This will delete the revision #2 which is with attachment
                assertNoRevisionAttachment(revisions.get(0), session, false);
                assertNoRevisionAttachment(revisions.get(1), session, true);
                assertRevisionAttachments(names, 3, revisions.get(2), session);
                assertRevisionAttachments(names, 2, revisions.get(3), session);

            }, 5, 1, 3);

            try (IDocumentSession session = store.openSession()) { // This will delete the revision #2 which is with attachment
                User user = new User();
                user.setName("Fitzchak 3");
                session.store(user, "users/1");
                session.saveChanges();
            }

            assertRevisions(store, names, (session, revisions) -> { // This will delete the revision #2 which is with attachment
                assertNoRevisionAttachment(revisions.get(0), session, false);
                assertNoRevisionAttachment(revisions.get(1), session, false);
                assertNoRevisionAttachment(revisions.get(2), session, true);
                assertRevisionAttachments(names, 3, revisions.get(3), session);

            }, 3, 1, 3);

            try (IDocumentSession session = store.openSession()) { // This will delete the revision #3 which is with attachment
                User user = new User();
                user.setName("Fitzchak 4");
                session.store(user, "users/1");
                session.saveChanges();
            }

            assertRevisions(store, names, (session, revisions) -> { // This will delete the revision #2 which is with attachment
                assertNoRevisionAttachment(revisions.get(0), session, false);
                assertNoRevisionAttachment(revisions.get(1), session, false);
                assertNoRevisionAttachment(revisions.get(2), session, false);
                assertNoRevisionAttachment(revisions.get(3), session, true);

            }, 0, 1, 0);

            try (IDocumentSession session = store.openSession()) { // This will delete the revision #4 which is with attachment
                User user = new User();
                user.setName("Fitzchak 5");
                session.store(user, "users/1");
                session.saveChanges();
            }

            assertRevisions(store, names, (session, revisions) -> { // This will delete the revision #2 which is with attachment
                assertNoRevisionAttachment(revisions.get(0), session, false);
                assertNoRevisionAttachment(revisions.get(1), session, false);
                assertNoRevisionAttachment(revisions.get(2), session, false);
                assertNoRevisionAttachment(revisions.get(3), session, false);

            }, 0, 1, 0);
        }
    }

    @Test
    public void attachmentRevision() throws Exception {
        try (DocumentStore store = getDocumentStore()) {
            setupRevisions(store, false, 4);

            String[] names = createDocumentWithAttachments(store);

            try (IDocumentSession session = store.openSession()) {
                ByteArrayInputStream bais = new ByteArrayInputStream(new byte[]{5, 4, 3, 2, 1});
                session.advanced().attachments().store("users/1", "profile.png", bais);

                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                List<User> revisions = session.advanced().revisions().getFor(User.class, "users/1");

                String changeVector = session.advanced().getChangeVectorFor(revisions.get(1));
                try (CloseableAttachmentResult revision = session.advanced().attachments().getRevision("users/1", "profile.png", changeVector)) {
                    byte[] bytes = IOUtils.toByteArray(revision.getData());
                    assertThat(bytes)
                            .hasSize(3)
                            .containsSequence(1, 2, 3);
                }
            }
        }
    }

    public static String[] createDocumentWithAttachments(DocumentStore store) {
        try (IDocumentSession session = store.openSession()) {
            User user = new User();
            user.setName("Fitzchak");
            session.store(user, "users/1");
            session.saveChanges();
        }

        String[] names =  new String[]
        {
            "profile.png",
                    "background-photo.jpg",
                    "fileNAME_#$1^%_בעברית.txt"
        };


        ByteArrayInputStream profileStream = new ByteArrayInputStream(new byte[]{1, 2, 3});
        AttachmentDetails result = store.operations().send(new PutAttachmentOperation("users/1", names[0], profileStream, "image/png"));
        assertThat(result.getChangeVector())
                .contains("A:3");
        assertThat(result.getName())
                .isEqualTo(names[0]);
        assertThat(result.getDocumentId())
                .isEqualTo("users/1");
        assertThat(result.getContentType())
                .isEqualTo("image/png");

        ByteArrayInputStream backgroundStream = new ByteArrayInputStream(new byte[]{10, 20, 30, 40, 50});
        result = store.operations().send(new PutAttachmentOperation("users/1", names[1], backgroundStream, "ImGgE/jPeG"));
        assertThat(result.getChangeVector())
                .contains("A:7");
        assertThat(result.getName())
                .isEqualTo(names[1]);
        assertThat(result.getDocumentId())
                .isEqualTo("users/1");
        assertThat(result.getContentType())
                .isEqualTo("ImGgE/jPeG");


        ByteArrayInputStream fileStream = new ByteArrayInputStream(new byte[]{ 1, 2, 3, 4, 5 });
        result = store.operations().send(new PutAttachmentOperation("users/1", names[2], fileStream, null));
        assertThat(result.getChangeVector())
                .contains("A:12");
        assertThat(result.getName())
                .isEqualTo(names[2]);
        assertThat(result.getDocumentId())
                .isEqualTo("users/1");
        assertThat(result.getContentType())
                .isEmpty();

        return names;
    }

    private static void assertRevisions(DocumentStore store, String[] names, BiConsumer<IDocumentSession, List<User>> assertAction, long expectedCountOfAttachments) {
        assertRevisions(store, names, assertAction,expectedCountOfAttachments , 1, 3);
    }

    private static void assertRevisions(DocumentStore store, String[] names, BiConsumer<IDocumentSession, List<User>> assertAction,
                                        long expectedCountOfAttachments, long expectedCountOfDocuments, long expectedCountOfUniqueAttachments) {
        DatabaseStatistics statistics = store.maintenance().send(new GetStatisticsOperation());

        assertThat(statistics.getCountOfAttachments())
                .isEqualTo(expectedCountOfAttachments);

        assertThat(statistics.getCountOfUniqueAttachments())
                .isEqualTo(expectedCountOfUniqueAttachments);

        assertThat(statistics.getCountOfRevisionDocuments())
                .isEqualTo(4);

        assertThat(statistics.getCountOfDocuments())
                .isEqualTo(expectedCountOfDocuments);

        assertThat(statistics.getCountOfIndexes())
                .isEqualTo(0);

        try (IDocumentSession session = store.openSession()) {
            List<User> revisions = session.advanced().revisions().getFor(User.class, "users/1");
            assertThat(revisions)
                    .hasSize(4);

            assertAction.accept(session, revisions);
        }
    }

    private static void assertNoRevisionAttachment(User revision, IDocumentSession session, boolean isDeleteRevision) {
        IMetadataDictionary metadata = session.advanced().getMetadataFor(revision);
        if (isDeleteRevision) {
            assertThat((String)metadata.get(Constants.Documents.Metadata.FLAGS))
                    .contains("HasRevisions")
                    .contains("DeleteRevision");
        } else {
            assertThat((String)metadata.get(Constants.Documents.Metadata.FLAGS))
                    .contains("HasRevisions")
                    .contains("Revision");
        }

        assertThat(metadata.containsKey(Constants.Documents.Metadata.ATTACHMENTS))
                .isFalse();
    }

    private static void assertRevisionAttachments(String[] names, int expectedCount, User revision, IDocumentSession session) {
        IMetadataDictionary metadata = session.advanced().getMetadataFor(revision);

        assertThat((String) metadata.get(Constants.Documents.Metadata.FLAGS))
                .contains("HasRevisions")
                .contains("Revision")
                .contains("HasAttachments");

        IMetadataDictionary[] attachments = metadata.getObjects(Constants.Documents.Metadata.ATTACHMENTS);
        assertThat(attachments)
                .hasSize(expectedCount);


        List<String> orderedNames = Arrays.stream(names).limit(expectedCount).sorted(Comparator.comparing(x -> x)).collect(Collectors.toList());

        for (int i = 0; i < expectedCount; i++) {
            String name = orderedNames.get(i);
            IMetadataDictionary attachment = attachments[i];

            assertThat(attachment.get("Name"))
                    .isEqualTo(name);

        }
    }
}
