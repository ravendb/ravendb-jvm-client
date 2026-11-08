package net.ravendb.client.test.client.attachments;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.BulkInsertOperation;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.attachments.AttachmentType;
import net.ravendb.client.documents.attachments.RemoteAttachmentFlags;
import net.ravendb.client.documents.attachments.RemoteAttachmentsConfiguration;
import net.ravendb.client.documents.attachments.RemoteAttachmentsDestinationConfiguration;
import net.ravendb.client.documents.attachments.RemoteAttachmentsS3Settings;
import net.ravendb.client.documents.commands.batches.PutAttachmentCommandData;
import net.ravendb.client.documents.operations.attachments.*;
import net.ravendb.client.documents.operations.attachments.remote.ConfigureRemoteAttachmentsOperation;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.extensions.JsonExtensions;
import net.ravendb.client.infrastructure.DisabledOnPullRequest;
import net.ravendb.client.infrastructure.EnableOnServer;
import net.ravendb.client.infrastructure.entities.User;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.StringWriter;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exercises the remote attachment client data path (RemoteParameters serialization / deserialization)
 * without a real cloud destination: a dummy S3 destination is configured and the upload is scheduled
 * far in the future, so the server never tries to upload.
 */
public class RemoteAttachmentsDataPathTest extends RemoteTestBase {

    private static final String IDENTIFIER = "s3-dest";

    private static Instant futureAt() {
        // the server keeps 100ns precision, truncate so we can compare exactly
        return Instant.now().plus(365, ChronoUnit.DAYS).truncatedTo(ChronoUnit.MILLIS);
    }

    private static void configureDummyDestination(IDocumentStore store) {
        RemoteAttachmentsS3Settings s3 = new RemoteAttachmentsS3Settings();
        s3.setBucketName("dummy-bucket");
        s3.setAwsRegionName("us-east-1");
        s3.setAwsAccessKey("dummy");
        s3.setAwsSecretKey("dummy");

        RemoteAttachmentsDestinationConfiguration destination = new RemoteAttachmentsDestinationConfiguration();
        destination.setS3Settings(s3);

        Map<String, RemoteAttachmentsDestinationConfiguration> destinations = new HashMap<>();
        destinations.put(IDENTIFIER, destination);

        RemoteAttachmentsConfiguration configuration = new RemoteAttachmentsConfiguration();
        configuration.setDestinations(destinations);

        store.maintenance().send(new ConfigureRemoteAttachmentsOperation(configuration));
    }

    private static void assertRemoteParameters(RemoteAttachmentParameters parameters, Instant at) {
        assertThat(parameters).isNotNull();
        assertThat(parameters.getIdentifier()).isEqualTo(IDENTIFIER);
        assertThat(parameters.getAt()).isEqualTo(at);
        assertThat(parameters.getFlags()).isEqualTo(RemoteAttachmentFlags.NONE);
    }

    private static void assertAllReadPaths(IDocumentStore store, String documentId, String name, Instant at) throws Exception {
        try (IDocumentSession session = store.openSession()) {
            User user = session.load(User.class, documentId);
            AttachmentName[] names = session.advanced().attachments().getNames(user);
            assertThat(names).hasSize(1);
            assertThat(names[0].getName()).isEqualTo(name);
            assertRemoteParameters(names[0].getRemoteParameters(), at);
        }

        try (CloseableAttachmentResult result = store.operations().send(
                new GetAttachmentOperation(documentId, name, AttachmentType.DOCUMENT, null))) {
            assertRemoteParameters(result.getDetails().getRemoteParameters(), at);
        }

        List<AttachmentRequest> requests = new ArrayList<>();
        requests.add(new AttachmentRequest(documentId, name));
        try (CloseableAttachmentsResult result = store.operations().send(
                new GetAttachmentsOperation(requests, AttachmentType.DOCUMENT))) {
            assertThat(result.hasNext()).isTrue();
            AttachmentIteratorResult item = result.next();
            assertRemoteParameters(item.getDetails().getRemoteParameters(), at);
        }
    }

    @Test
    public void putAttachmentCommandDataSerializesRemoteParameters() throws Exception {
        RemoteAttachmentParameters parameters = new RemoteAttachmentParameters(IDENTIFIER, Instant.parse("2030-01-02T03:04:05.123Z"));
        PutAttachmentCommandData command = new PutAttachmentCommandData("users/1", "a.txt",
                new ByteArrayInputStream(new byte[]{1}), "text/plain", null, parameters);

        ObjectMapper mapper = JsonExtensions.getDefaultMapper();
        StringWriter writer = new StringWriter();
        try (JsonGenerator generator = mapper.getFactory().createGenerator(writer)) {
            command.serialize(generator, null);
        }

        JsonNode json = mapper.readTree(writer.toString());
        assertThat(json.get("Type").asText()).isEqualTo("AttachmentPUT");
        assertThat(json.get("FromEtl").asBoolean()).isFalse();
        JsonNode remote = json.get("RemoteParameters");
        assertThat(remote.get("At").asText()).isEqualTo("2030-01-02T03:04:05.1230000Z");
        assertThat(remote.get("Identifier").asText()).isEqualTo(IDENTIFIER);
        assertThat(remote.get("Flags").asText()).isEqualTo("None");

        PutAttachmentCommandData local = new PutAttachmentCommandData("users/1", "a.txt",
                new ByteArrayInputStream(new byte[]{1}), null, null);
        writer = new StringWriter();
        try (JsonGenerator generator = mapper.getFactory().createGenerator(writer)) {
            local.serialize(generator, null);
        }
        assertThat(mapper.readTree(writer.toString()).get("RemoteParameters").isNull()).isTrue();
    }

    @Test
    public void defaultMapperRoundTripsRemoteParameters() throws Exception {
        ObjectMapper mapper = JsonExtensions.getDefaultMapper();

        String serverJson = "{\"Name\":\"a.txt\",\"Hash\":\"h\",\"ContentType\":\"\",\"Size\":3," +
                "\"RemoteParameters\":{\"At\":\"2030-01-02T03:04:05.1234567Z\",\"Identifier\":\"s3-dest\",\"Flags\":\"Remote\"}}";
        AttachmentName name = mapper.readValue(serverJson, AttachmentName.class);
        assertThat(name.getRemoteParameters().getAt()).isEqualTo(Instant.parse("2030-01-02T03:04:05.123456700Z"));
        assertThat(name.getRemoteParameters().getIdentifier()).isEqualTo(IDENTIFIER);
        assertThat(name.getRemoteParameters().getFlags()).isEqualTo(RemoteAttachmentFlags.REMOTE);

        // value without zone designator is treated as UTC
        RemoteAttachmentParameters unspecified = mapper.readValue(
                "{\"At\":\"2030-01-02T03:04:05.1234567\",\"Identifier\":\"x\",\"Flags\":\"None\"}", RemoteAttachmentParameters.class);
        assertThat(unspecified.getAt()).isEqualTo(Instant.parse("2030-01-02T03:04:05.123456700Z"));

        RemoteAttachmentParameters parameters = new RemoteAttachmentParameters(IDENTIFIER, Instant.parse("2030-01-02T03:04:05Z"));
        JsonNode json = mapper.valueToTree(parameters);
        assertThat(json.get("At").asText()).isEqualTo("2030-01-02T03:04:05.0000000Z");
        assertThat(json.get("Identifier").asText()).isEqualTo(IDENTIFIER);
        assertThat(json.get("Flags").asText()).isEqualTo("None");
    }

    @EnableOnServer(thresholdVersion = "7.2")
    @DisabledOnPullRequest
    @Test
    public void canStoreRemoteAttachmentUsingSession() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            configureDummyDestination(store);
            Instant at = futureAt();

            try (IDocumentSession session = store.openSession()) {
                User user = new User();
                user.setName("Grisha");
                session.store(user, "users/1");

                StoreAttachmentParameters parameters = new StoreAttachmentParameters("a.txt",
                        new ByteArrayInputStream(new byte[]{1, 2, 3}), "text/plain");
                parameters.setRemoteParameters(new RemoteAttachmentParameters(IDENTIFIER, at));
                session.advanced().attachments().store("users/1", parameters);
                session.saveChanges();
            }

            assertAllReadPaths(store, "users/1", "a.txt", at);
        }
    }

    @EnableOnServer(thresholdVersion = "7.2")
    @DisabledOnPullRequest
    @Test
    public void canStoreRemoteAttachmentUsingPutAttachmentOperation() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            configureDummyDestination(store);
            Instant at = futureAt();

            try (IDocumentSession session = store.openSession()) {
                session.store(new User(), "users/1");
                session.saveChanges();
            }

            StoreAttachmentParameters parameters = new StoreAttachmentParameters("a.txt",
                    new ByteArrayInputStream(new byte[]{1, 2, 3}), "text/plain");
            parameters.setRemoteParameters(new RemoteAttachmentParameters(IDENTIFIER, at));

            AttachmentDetails details = store.operations().send(new PutAttachmentOperation("users/1", parameters));
            assertThat(details.getName()).isEqualTo("a.txt");
            assertRemoteParameters(details.getRemoteParameters(), at);

            assertAllReadPaths(store, "users/1", "a.txt", at);
        }
    }

    @EnableOnServer(thresholdVersion = "7.2")
    @DisabledOnPullRequest
    @Test
    public void canStoreRemoteAttachmentUsingBulkInsert() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            configureDummyDestination(store);
            Instant at = futureAt();
            Instant at2 = at.plus(1, ChronoUnit.DAYS);

            try (BulkInsertOperation bulkInsert = store.bulkInsert()) {
                bulkInsert.store(new User(), "users/1");
                bulkInsert.attachmentsFor("users/1").store("a.txt", new byte[]{1, 2, 3}, "text/plain",
                        new RemoteAttachmentParameters(IDENTIFIER, at));

                bulkInsert.store(new User(), "users/2");
                StoreAttachmentParameters parameters = new StoreAttachmentParameters("b.txt",
                        new ByteArrayInputStream(new byte[]{4, 5}));
                parameters.setRemoteParameters(new RemoteAttachmentParameters(IDENTIFIER, at2));
                bulkInsert.attachmentsFor("users/2").store(parameters);
            }

            assertAllReadPaths(store, "users/1", "a.txt", at);
            assertAllReadPaths(store, "users/2", "b.txt", at2);
        }
    }
}
