package net.ravendb.client.test.client.attachments;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.BulkInsertOperation;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.operations.attachments.AttachmentIteratorResult;
import net.ravendb.client.documents.operations.attachments.AttachmentRequest;
import net.ravendb.client.documents.operations.attachments.CloseableAttachmentsResult;
import net.ravendb.client.documents.operations.counters.CountersDetail;
import net.ravendb.client.documents.operations.counters.GetCountersOperation;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.exceptions.BulkInsertInvalidOperationException;
import net.ravendb.client.infrastructure.entities.User;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.util.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.*;

public class BulkInsertAttachmentsTest extends RemoteTestBase {

    @Test
    public void storeManyAttachments1() throws Exception {
        storeManyAttachments(1, 32 * 1024);
    }

    @Test
    public void storeManyAttachments2() throws Exception {
        storeManyAttachments(100, 256 * 1024);
    }

    @Test
    public void storeManyAttachments3() throws Exception {
        storeManyAttachments(200, 128 * 1024);
    }

    @Test
    public void storeManyAttachments4() throws Exception {
        storeManyAttachments(1000, 16 * 1024);
    }

    public void storeManyAttachments(int count, int size) throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            String userId = "user/1";

            Map<String, ByteArrayInputStream> streams = new HashMap<>();

            try (BulkInsertOperation bulkInsert = store.bulkInsert()) {
                User user1 = new User();
                user1.setName("EGR");

                bulkInsert.store(user1, userId);

                BulkInsertOperation.AttachmentsBulkInsert attachmentsBulkInsert = bulkInsert.attachmentsFor(userId);

                for (int i = 0; i < count; i++) {
                    Random rnd = new Random();
                    byte[] bArr = new byte[size];
                    rnd.nextBytes(bArr);

                    String name = String.valueOf(i);
                    ByteArrayInputStream stream = new ByteArrayInputStream(bArr);

                    attachmentsBulkInsert.store(name, bArr);

                    streams.put(name, stream);
                }
            }

            try (IDocumentSession session = store.openSession()) {
                List<AttachmentRequest> attachmentsNames = streams
                        .keySet()
                        .stream()
                        .map(byteArrayInputStream -> new AttachmentRequest(userId, byteArrayInputStream))
                        .collect(Collectors.toList());

                int counter = 0;

                try (CloseableAttachmentsResult attachmentsResult = session.advanced().attachments().get(attachmentsNames)) {
                    while (attachmentsResult.hasNext()) {
                        counter++;
                        AttachmentIteratorResult result = attachmentsResult.next();
                        assertThat(result)
                                .isNotNull();
                        assertThat(AttachmentsStreamTest.compareStreams(result.getStream(), streams.get(result.getDetails().getName())))
                                .isTrue();
                    }
                }

                assertThat(counter)
                        .isEqualTo(count);
            }
        }
    }

    @Test
    public void storeManyAttachmentsAndDocs() throws Exception {
        int count = 100;
        int attachments = 100;
        int size = 16 * 1024;

        try (IDocumentStore store = getDocumentStore()) {
            Map<String, Map<String, byte[]>> streams = new HashMap<>();

            try (BulkInsertOperation bulkInsert = store.bulkInsert()) {
                for (int i = 0; i < count; i++) {
                    String id = "user/" + i;

                    streams.put(id, new HashMap<>());

                    User user = new User();
                    user.setName("EGR_" + i);
                    bulkInsert.store(user, id);

                    BulkInsertOperation.AttachmentsBulkInsert attachmentsBulkInsert = bulkInsert.attachmentsFor(id);

                    for (int j = 0; j < attachments; j++) {
                        Random rnd = new Random();
                        byte[] bArr = new byte[size];
                        rnd.nextBytes(bArr);
                        String name = String.valueOf(j);
                        attachmentsBulkInsert.store(name, bArr);

                        streams.get(id).put(name, bArr);
                    }
                }
            }

            for (String id : streams.keySet()) {
                try (IDocumentSession session = store.openSession()) {
                    List<AttachmentRequest> attachmentsNames = streams
                            .keySet()
                            .stream()
                            .map(x -> new AttachmentRequest(id, x))
                            .collect(Collectors.toList());

                    try (CloseableAttachmentsResult attachmentsResult = session.advanced().attachments().get(attachmentsNames)) {
                        while (attachmentsResult.hasNext()) {
                            AttachmentIteratorResult result = attachmentsResult.next();

                            assertThat(result)
                                    .isNotNull();

                            assertThat(AttachmentsStreamTest.compareStreams(result.getStream(),
                                    new ByteArrayInputStream(streams.get(id).get(result.getDetails().getName()))))
                                    .isTrue();
                        }
                    }
                }
            }
        }
    }

    @Test
    public void bulkStoreAttachmentsForRandomDocs() throws Exception {
        int count = 500;
        int attachments = 750;
        int size = 16 * 1024;

        try (IDocumentStore store = getDocumentStore()) {
            Map<String, Map<String, byte[]>> streams = new HashMap<>();

            List<String> ids = new ArrayList<>();

            try (BulkInsertOperation bulkInsert = store.bulkInsert()) {
                for (int i = 0; i < count; i++) {
                    String id = "user/" + i;
                    ids.add(id);
                    streams.put(id, new HashMap<>());
                    User user = new User();
                    user.setName("EGR_" + i);
                    bulkInsert.store(user, id);
                }


                for (int j = 0; j < attachments; j++) {
                    Random rnd = new Random();
                    String id = ids.get(rnd.nextInt(count));
                    BulkInsertOperation.AttachmentsBulkInsert attachmentsBulkInsert = bulkInsert.attachmentsFor(id);

                    byte[] bArr = new byte[size];
                    rnd.nextBytes(bArr);

                    String name = String.valueOf(j);
                    attachmentsBulkInsert.store(name, bArr);

                    streams.get(id).put(name, bArr);
                }
            }

            for (String id : streams.keySet()) {
                try (IDocumentSession session = store.openSession()) {
                    List<AttachmentRequest> attachmentNames = streams
                            .keySet()
                            .stream()
                            .map(x -> new AttachmentRequest(id, x))
                            .collect(Collectors.toList());

                    try (CloseableAttachmentsResult attachmentsResult = session.advanced().attachments().get(attachmentNames)) {
                        while (attachmentsResult.hasNext()) {
                            AttachmentIteratorResult result = attachmentsResult.next();

                            assertThat(result)
                                    .isNotNull();

                            assertThat(AttachmentsStreamTest.compareStreams(result.getStream(),
                                    new ByteArrayInputStream(streams.get(id).get(result.getDetails().getName()))))
                                    .isTrue();
                        }
                    }
                }
            }
        }
    }

    @Test
    public void canHaveAttachmentBulkInsertsWithCounters() throws Exception {
        int count = 100;
        int size = 64 * 1024;

        try (IDocumentStore store = getDocumentStore()) {
            Map<String, Map<String, byte[]>> streams = new HashMap<>();
            Map<String, String> counters = new HashMap<>();
            Map<String, BulkInsertOperation.AttachmentsBulkInsert> bulks = new HashMap<>();

            try (BulkInsertOperation bulkInsert = store.bulkInsert()) {
                for (int i = 0; i < count; i++) {
                    String id = "user/" + i;
                    streams.put(id, new HashMap<>());
                    User user = new User();
                    user.setName("EGR_" + i);
                    bulkInsert.store(user, id);

                    bulks.put(id, bulkInsert.attachmentsFor(id));
                }

                for (Map.Entry<String, BulkInsertOperation.AttachmentsBulkInsert> bulk : bulks.entrySet()) {
                    Random rnd = new Random();
                    byte[] bArr = new byte[size];
                    rnd.nextBytes(bArr);

                    String name = bulk.getKey() + "_" + rnd.nextInt(100);
                    bulk.getValue().store(name, bArr);

                    bulkInsert.countersFor(bulk.getKey()).increment(name);
                    counters.put(bulk.getKey(), name);
                }
            }

            for (String id : streams.keySet()) {
                try (IDocumentSession session = store.openSession()) {
                    List<AttachmentRequest> attachmentsNames = streams
                            .keySet()
                            .stream()
                            .map(x -> new AttachmentRequest(id, x))
                            .collect(Collectors.toList());
                    try (CloseableAttachmentsResult closeableAttachmentsResult = session.advanced().attachments().get(attachmentsNames)) {

                        while (closeableAttachmentsResult.hasNext()) {
                            AttachmentIteratorResult result = closeableAttachmentsResult.next();

                            assertThat(result)
                                    .isNotNull();

                            assertThat(AttachmentsStreamTest.compareStreams(result.getStream(),
                                    new ByteArrayInputStream(streams.get(id).get(result.getDetails().getName()))))
                                    .isTrue();
                        }
                    }

                }

                CountersDetail countersDetail = store.operations().send(new GetCountersOperation(id, new String[]{counters.get(id)}));
                assertThat(countersDetail.getCounters().get(0).getTotalValue())
                        .isEqualTo(1);
            }
        }
    }

    @Test
    public void storeAsyncShouldThrowIfRunningTimeSeriesBulkInsert() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            assertThatThrownBy(() -> {
                try (BulkInsertOperation bulkInsert = store.bulkInsert()) {
                    bulkInsert.timeSeriesFor("id", "name");

                    BulkInsertOperation.AttachmentsBulkInsert bulk = bulkInsert.attachmentsFor("id");

                    bulk.store("name", new byte[5]);
                }
            })
                    .isInstanceOf(BulkInsertInvalidOperationException.class)
                    .hasMessageContaining("There is an already running time series operation, did you forget to close it?");
        }
    }

    @Test
    public void storeAsyncNullId() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            assertThatThrownBy(() -> {
                try (BulkInsertOperation bulkInsert = store.bulkInsert()) {
                    bulkInsert.attachmentsFor(null);
                }
            }).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Document id cannot be null or empty");
        }
    }

}
