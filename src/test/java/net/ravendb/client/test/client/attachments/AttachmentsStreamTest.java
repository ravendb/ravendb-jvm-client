package net.ravendb.client.test.client.attachments;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.operations.attachments.*;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.infrastructure.entities.User;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class AttachmentsStreamTest extends RemoteTestBase {

    @Test
    public void canGetOneAttachment1() throws Exception {
        canGetOneAttachment(1024);
    }

    @Test
    public void canGetOneAttachment2() throws Exception {
        canGetOneAttachment(1024 * 1024);
    }

    @Test
    public void canGetOneAttachment3() throws Exception {
        canGetOneAttachment(128 * 1024 * 1024);
    }

    private void canGetOneAttachment(int size) throws Exception {
        Random rnd = new Random();
        byte[] b = new byte[size];
        rnd.nextBytes(b);

        try (ByteArrayInputStream stream = new ByteArrayInputStream(b)) {
            String id = "users/1";
            String attachmentName = "Typical attachment name";

            try (IDocumentStore store = getDocumentStore()) {
                try (IDocumentSession session = store.openSession()) {
                    User user = new User();
                    user.setName("su");
                    session.store(user, id);
                    session.saveChanges();
                }

                store.operations().send(new PutAttachmentOperation(id, attachmentName, stream, "application/zip"));

                stream.reset();

                try (IDocumentSession session = store.openSession()) {
                    User user = session.load(User.class, id);
                    List<AttachmentRequest> attachmentsNames = Arrays.stream(session.advanced().attachments().getNames(user))
                            .map(x -> new AttachmentRequest(id, x.getName()))
                            .collect(Collectors.toList());

                    try (CloseableAttachmentsResult attachmentsResult = session.advanced().attachments().get(attachmentsNames)) {
                        while (attachmentsResult.hasNext()) {
                            AttachmentIteratorResult item = attachmentsResult.next();
                            assertThat(compareStreams(item.getStream(), stream))
                                    .isTrue();
                        }
                    }
                }
            }
        }
    }

    @Test
    public void canConsumeStream1() throws Exception {
        canConsumeStream(1);
    }

    @Test
    public void canConsumeStream2() throws Exception {
        canConsumeStream(10);
    }

    @Test
    public void canConsumeStream3() throws Exception {
        canConsumeStream(100);
    }

    private void canConsumeStream(int count) throws Exception {
        int size = 32 * 1024;

        Map<String, ByteArrayInputStream> attachmentDictionary = new HashMap<>();
        String id = "users/1";
        String attachmentName = "Typical attachment name";

        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                User user = new User();
                user.setName("su");
                session.store(user, id);

                for (int i = 0; i < count; i++) {
                    Random rnd = new Random();
                    byte[] b = new byte[size];
                    rnd.nextBytes(b);

                    ByteArrayInputStream stream = new ByteArrayInputStream(b);
                    session.advanced().attachments().store(id, attachmentName + "_" + i, stream, "application/zip");
                    attachmentDictionary.put(attachmentName + "_" + i, stream);
                }

                session.saveChanges();
            }

            for (ByteArrayInputStream s : attachmentDictionary.values()) {
                s.reset();
            }

            try (IDocumentSession session = store.openSession()) {
                User user = session.load(User.class, id);
                List<AttachmentRequest> attachmentNames = Arrays.stream(session.advanced().attachments().getNames(user))
                        .map(x -> new AttachmentRequest(id, x.getName()))
                        .collect(Collectors.toList());

                try (CloseableAttachmentsResult closeableAttachmentsResult = session.advanced().attachments().get(attachmentNames)) {
                    while (closeableAttachmentsResult.hasNext()) {
                        AttachmentIteratorResult iteratorResult = closeableAttachmentsResult.next();

                        ByteArrayOutputStream memoryStream = new ByteArrayOutputStream();
                        IOUtils.copy(iteratorResult.getStream(), memoryStream);

                        ByteArrayOutputStream memoryStream2 = new ByteArrayOutputStream();
                        IOUtils.copy(iteratorResult.getStream(), memoryStream2);
                        assertThat(memoryStream2.size())
                                .isZero();

                        byte[] buffer1 = new byte[size];

                        assertThat(attachmentDictionary.get(iteratorResult.getDetails().getName())
                                .read(buffer1))
                                .isEqualTo(size);
                        byte[] buffer2 = memoryStream.toByteArray();

                        assertThat(buffer2.length)
                                .isEqualTo(buffer1.length);

                        assertThat(buffer2)
                                .containsSequence(buffer1);
                    }
                }
            }
        }
    }


    @Test
    public void canGetListOfAttachmentsAndSkip1() throws Exception {
        canGetListOfAttachmentsAndSkip(10, 3);
    }

    @Test
    public void canGetListOfAttachmentsAndSkip2() throws Exception {
        canGetListOfAttachmentsAndSkip(1, 32768);
    }

    @Test
    public void canGetListOfAttachmentsAndSkip3() throws Exception {
        canGetListOfAttachmentsAndSkip(10, 32768);
    }

    @Test
    public void canGetListOfAttachmentsAndSkip4() throws Exception {
        canGetListOfAttachmentsAndSkip(100, 3);
    }

    private void canGetListOfAttachmentsAndSkip(int count, int size) throws Exception {
        Map<String, ByteArrayInputStream> attachmentDictionary = new HashMap<>();
        String id = "users/1";
        String attachmentName = "Typical attachment name";

        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                User user = new User();
                user.setName("su");
                session.store(user, id);

                for (int i = 0; i < count; i++) {
                    Random rnd = new Random();
                    byte[] b = new byte[size];
                    rnd.nextBytes(b);

                    ByteArrayInputStream stream = new ByteArrayInputStream(b);
                    session.advanced().attachments().store(id, attachmentName + "_" + i, stream, "application/zip");
                    attachmentDictionary.put(attachmentName + "_" + i, stream);
                }

                session.saveChanges();
            }

            for (ByteArrayInputStream s : attachmentDictionary.values()) {
                s.reset();
            }

            try (IDocumentSession session = store.openSession()) {
                User user = session.load(User.class, id);

                List<AttachmentRequest> attachmentNames = Arrays.stream(session.advanced().attachments().getNames(user))
                        .map(x -> new AttachmentRequest(id, x.getName()))
                        .collect(Collectors.toList());

                try (CloseableAttachmentsResult closeableAttachmentsResult = session.advanced().attachments().get(attachmentNames)) {
                    Random rndRnd = new Random();

                    while (closeableAttachmentsResult.hasNext()) {
                        AttachmentIteratorResult result = closeableAttachmentsResult.next();
                        if (rndRnd.nextInt(2) == 0) {
                            continue;
                        }

                        assertThat(result)
                                .isNotNull();
                        assertThat(compareStreams(result.getStream(), attachmentDictionary.get(result.getDetails().getName())))
                                .isTrue();
                    }
                }
            }
        }
    }

    @Test
    public void shouldThrowOnDisposedStream() throws Exception {
        int count = 10;
        int size = 3;
        Map<String, ByteArrayInputStream> attachmentDictionary = new HashMap<>();
        String id = "users/1";
        String attachmentName = "Typical attachment name";

        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                User user = new User();
                user.setName("su");
                session.store(user, id);

                for (int i = 0; i < count; i++) {
                    Random rnd = new Random();
                    byte[] b = new byte[size];
                    rnd.nextBytes(b);

                    ByteArrayInputStream stream = new ByteArrayInputStream(b);
                    session.advanced().attachments().store(id, attachmentName + "_" + i, stream, "application/zip");
                    attachmentDictionary.put(attachmentName + "_" + i, stream);
                }

                session.saveChanges();
            }

            for (ByteArrayInputStream s : attachmentDictionary.values()) {
                s.reset();
            }

            try (IDocumentSession session = store.openSession()) {
                User user = session.load(User.class, id);

                List<AttachmentRequest> attachmentNames = Arrays.stream(session.advanced().attachments().getNames(user))
                        .map(x -> new AttachmentRequest(id, x.getName()))
                        .collect(Collectors.toList());

                CloseableAttachmentsResult closeableAttachmentsResult = session.advanced().attachments().get(attachmentNames);
                AttachmentIteratorResult result = null;
                try {
                    while (closeableAttachmentsResult.hasNext()) {
                        result = closeableAttachmentsResult.next();
                    }
                } finally {
                    closeableAttachmentsResult.close();
                }

                AttachmentIteratorResult finalResult = result;
                assertThatThrownBy(() -> finalResult.getStream().read())
                        .isInstanceOf(IOException.class)
                        .hasMessage("Attempted read on closed stream.");
            }
        }
    }

    @Test
    public void canGetListOfAttachmentsAndReadPartially1() throws Exception {
        canGetListOfAttachmentsAndReadPartially(10, 3);
    }

    @Test
    public void canGetListOfAttachmentsAndReadPartially2() throws Exception {
        canGetListOfAttachmentsAndReadPartially(1, 32768);
    }

    @Test
    public void canGetListOfAttachmentsAndReadPartially3() throws Exception {
        canGetListOfAttachmentsAndReadPartially(10, 32768);
    }

    @Test
    public void canGetListOfAttachmentsAndReadPartially4() throws Exception {
        canGetListOfAttachmentsAndReadPartially(100, 3);
    }

    private void canGetListOfAttachmentsAndReadPartially(int count, int size) throws Exception {
        Map<String, ByteArrayInputStream> attachmentDictionary = new HashMap<>();
        String id = "users/1";
        String attachmentName = "Typical attachment name";

        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                User user = new User();
                user.setName("su");
                session.store(user, id);

                for (int i = 0; i < count; i++) {
                    Random rnd = new Random();
                    byte[] b = new byte[size];
                    rnd.nextBytes(b);

                    ByteArrayInputStream stream = new ByteArrayInputStream(b);
                    session.advanced().attachments().store(id, attachmentName + "_" + i, stream, "application/zip");
                    attachmentDictionary.put(attachmentName + "_" + i, stream);
                }

                session.saveChanges();
            }

            for (ByteArrayInputStream s : attachmentDictionary.values()) {
                s.reset();
            }

            try (IDocumentSession session = store.openSession()) {
                User user = session.load(User.class, id);

                List<AttachmentRequest> attachmentNames = Arrays.stream(session.advanced().attachments().getNames(user))
                        .map(x -> new AttachmentRequest(id, x.getName()))
                        .collect(Collectors.toList());

                Random rndRnd = new Random();

                try (CloseableAttachmentsResult closeableAttachmentsResult = session.advanced().attachments().get(attachmentNames)) {
                    while (closeableAttachmentsResult.hasNext()) {
                        AttachmentIteratorResult item = closeableAttachmentsResult.next();

                        if (rndRnd.nextInt(2) == 0) {
                            int s = size / 3;
                            byte[] buffer1 = new byte[s];
                            byte[] buffer2 = new byte[s];

                            IOUtils.readFully(item.getStream(), buffer1, 0, s);

                            assertThat(attachmentDictionary.get(item.getDetails().getName()).read(buffer2, 0, s))
                                    .isEqualTo(s);
                            assertThat(buffer1)
                                    .containsSequence(buffer2);
                        } else {
                            assertThat(compareStreams(item.getStream(), attachmentDictionary.get(item.getDetails().getName())))
                                    .isTrue();
                        }
                    }
                }
            }
        }
    }

    @Test
    public void canSendNonExistingListOfAttachments() throws Exception {
        Random rnd = new Random();
        byte[] b = new byte[rnd.nextInt(64 * 1024) + 1];
        rnd.nextBytes(b);

        String id = "users/1";
        String attachmentName = "Typical attachment name";

        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                User user = new User();
                user.setName("su");
                session.store(user, id);
                session.advanced().attachments().store(id, attachmentName, new ByteArrayInputStream(b), "application/zip");
                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                try (CloseableAttachmentsResult closeableAttachmentsResult = session.advanced().attachments().get(new ArrayList<>())) {
                    assertThat(closeableAttachmentsResult.hasNext())
                            .isFalse();
                }
            }

            try (IDocumentSession session = store.openSession()) {
                AttachmentRequest attachmentRequest = new AttachmentRequest("users/1", "1");
                try (CloseableAttachmentsResult closeableAttachmentsResult = session.advanced().attachments().get(Collections.singletonList(attachmentRequest))) {
                    assertThat(closeableAttachmentsResult.hasNext())
                            .isFalse();
                }
            }

            try (IDocumentSession session = store.openSession()) {
                AttachmentRequest attachmentRequest = new AttachmentRequest("users/2", "1");
                try (CloseableAttachmentsResult closeableAttachmentsResult = session.advanced().attachments().get(Collections.singletonList(attachmentRequest))) {
                    assertThat(closeableAttachmentsResult.hasNext())
                            .isFalse();
                }
            }

            try (IDocumentSession session = store.openSession()) {
                AttachmentRequest attachmentRequest1 = new AttachmentRequest("users/1", "1");
                AttachmentRequest attachmentRequest2 = new AttachmentRequest("users/1", attachmentName);
                AttachmentRequest attachmentRequest3 = new AttachmentRequest("users/2", "1");

                List<AttachmentRequest> attachmentRequests = Arrays.asList(attachmentRequest1, attachmentRequest2, attachmentRequest3);

                try (CloseableAttachmentsResult closeableAttachmentsResult = session.advanced().attachments().get(attachmentRequests)) {
                    assertThat(closeableAttachmentsResult.hasNext())
                            .isTrue();

                    AttachmentIteratorResult result = closeableAttachmentsResult.next();
                    assertThat(result.getDetails().getName())
                            .isEqualTo(attachmentName);
                    assertThat(closeableAttachmentsResult.hasNext())
                            .isFalse();
                }
            }
        }
    }

    public static boolean compareStreams(InputStream a, InputStream b) throws IOException {
        return IOUtils.contentEquals(a, b);
    }

}
