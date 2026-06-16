package net.ravendb.client.test.issues;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import net.ravendb.client.Constants;
import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.DocumentStore;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.commands.batches.BatchTrackChangesCommandData;
import net.ravendb.client.documents.commands.batches.ICommandData;
import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.session.DocumentInfo;
import net.ravendb.client.documents.session.EntityToJson;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.documents.session.InMemoryDocumentSessionOperations;
import net.ravendb.client.documents.session.OptimisticConcurrencyMode;
import net.ravendb.client.documents.session.SessionOptions;
import net.ravendb.client.exceptions.ConcurrencyException;
import net.ravendb.client.extensions.JsonExtensions;
import net.ravendb.client.infrastructure.EnableOnServer;
import net.ravendb.client.infrastructure.entities.Address;
import net.ravendb.client.primitives.Reference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Date;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;

/**
 * Port of the C# SlowTests.Issues.RavenDB_25154 test, covering the
 * {@link OptimisticConcurrencyMode#WRITES_AND_READS} feature (read-tracking + BatchTrackChanges).
 *
 * Requires a server >= 7.2.2 (the server-side BatchTrackChangesCommand), so the whole class is gated.
 *
 * Not ported from the C# original:
 *  - All *Async tests / SessionAsyncActions: the Java client has no async session API.
 *  - WritesAndReads_ShouldThrowOnShardedDatabase: the Java test infrastructure has no sharded database mode.
 */
@EnableOnServer(thresholdVersion = "7.2")
public class RavenDB_25154Test extends RemoteTestBase {

    private static final String JERRY_ID = "employees/1-A";
    private static final String EGOR_ID = "employees/2-A";

    public static class Employee {
        private String id;
        private String firstName;
        private Address address;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getFirstName() {
            return firstName;
        }

        public void setFirstName(String firstName) {
            this.firstName = firstName;
        }

        public Address getAddress() {
            return address;
        }

        public void setAddress(Address address) {
            this.address = address;
        }
    }

    // region session actions (mirror C# SessionActions())

    public static class NamedAction {
        private final String name;
        private final Consumer<IDocumentSession> action;

        public NamedAction(String name, Consumer<IDocumentSession> action) {
            this.name = name;
            this.action = action;
        }

        public void invoke(IDocumentSession session) {
            action.accept(session);
        }

        @Override
        public String toString() {
            return name;
        }
    }

    public static Stream<NamedAction> sessionActions() {
        return Stream.of(
                new NamedAction("ModifyEgorInSession", RavenDB_25154Test::modifyEgorInSession),
                new NamedAction("ModifyEgorInPatchAfterLoad", RavenDB_25154Test::modifyEgorInPatchAfterLoad),
                new NamedAction("ModifyEgorInPatchNoLoad", RavenDB_25154Test::modifyEgorInPatchNoLoad),
                new NamedAction("ModifyEgorWithStoreOverwriteAfterLoadAndEvict", RavenDB_25154Test::modifyEgorWithStoreOverwriteAfterLoadAndEvict),
                new NamedAction("ModifyEgorWithStoreOverwriteWithoutLoad", RavenDB_25154Test::modifyEgorWithStoreOverwriteWithoutLoad),
                new NamedAction("ModifyEgorWithLoadAndDeleteById", RavenDB_25154Test::modifyEgorWithLoadAndDeleteById),
                new NamedAction("ModifyEgorWithLoadAndDeleteByEntity", RavenDB_25154Test::modifyEgorWithLoadAndDeleteByEntity),
                new NamedAction("ModifyEgorWithMultiplePatches", RavenDB_25154Test::modifyEgorWithMultiplePatches),
                new NamedAction("ModifyEgorByReplacingAddress", RavenDB_25154Test::modifyEgorByReplacingAddress),
                new NamedAction("ModifyEgorWithAttachmentWithLoad", RavenDB_25154Test::modifyEgorWithAttachmentWithLoad),
                new NamedAction("ModifyEgorWithAttachmentNoLoad", RavenDB_25154Test::modifyEgorWithAttachmentNoLoad),
                new NamedAction("ModifyEgorWithTimeSeriesNoLoad", RavenDB_25154Test::modifyEgorWithTimeSeriesNoLoad),
                new NamedAction("ModifyEgorWithTimeSeriesWithLoad", RavenDB_25154Test::modifyEgorWithTimeSeriesWithLoad),
                new NamedAction("ModifyEgorWithCounterWithLoad", RavenDB_25154Test::modifyEgorWithCounterWithLoad),
                new NamedAction("ModifyEgorWithCounterNoLoad", RavenDB_25154Test::modifyEgorWithCounterNoLoad),
                new NamedAction("ModifyEgorWithRevisionNoLoad", RavenDB_25154Test::modifyEgorWithRevisionNoLoad),
                new NamedAction("ModifyEgorWithRevisionWithLoad", RavenDB_25154Test::modifyEgorWithRevisionWithLoad)
        );
    }

    private static InputStream imageStream() {
        return new ByteArrayInputStream("image data".getBytes(StandardCharsets.UTF_8));
    }

    private static Address street(String value) {
        Address address = new Address();
        address.setStreet(value);
        return address;
    }

    private static void modifyEgorInPatchAfterLoad(IDocumentSession session) {
        Employee egor = session.load(Employee.class, EGOR_ID);
        session.advanced().patch(egor, "address.street", "Mul HaHof Village");
    }

    private static void modifyEgorInPatchNoLoad(IDocumentSession session) {
        session.advanced().patch(EGOR_ID, "address.street", "Mul HaHof Village");
    }

    private static void modifyEgorInSession(IDocumentSession session) {
        Employee egor = session.load(Employee.class, EGOR_ID);
        egor.setAddress(street("Mul HaHof Village"));
    }

    private static void modifyEgorWithStoreOverwriteAfterLoadAndEvict(IDocumentSession session) {
        Employee egor = session.load(Employee.class, EGOR_ID);

        // tracked entity is evicted from the session, so it's not in the trackedEntities list anymore
        session.advanced().evict(egor);
        // store after evict is treated as a _new_ document, so it will be added to the trackedEntities list again with
        // a null change vector, and this should not cause a concurrency exception because the original entity is evicted!
        Employee newEgor = new Employee();
        newEgor.setFirstName("Egor");
        newEgor.setAddress(street("Mul HaHof Village"));
        session.store(newEgor, EGOR_ID);
    }

    private static void modifyEgorWithStoreOverwriteWithoutLoad(IDocumentSession session) {
        Employee newEgor = new Employee();
        newEgor.setFirstName("Egor");
        newEgor.setAddress(street("Mul HaHof Village"));
        session.store(newEgor, EGOR_ID);
    }

    private static void modifyEgorWithLoadAndDeleteById(IDocumentSession session) {
        session.load(Employee.class, EGOR_ID);
        session.delete(EGOR_ID);
    }

    private static void modifyEgorWithLoadAndDeleteByEntity(IDocumentSession session) {
        Employee egor = session.load(Employee.class, EGOR_ID);
        session.delete(egor);
    }

    private static void modifyEgorWithMultiplePatches(IDocumentSession session) {
        session.advanced().patch(EGOR_ID, "address.street", "Mul HaHof Village");
        session.advanced().patch(EGOR_ID, "firstName", "Egor");
    }

    private static void modifyEgorByReplacingAddress(IDocumentSession session) {
        Employee egor = session.load(Employee.class, EGOR_ID);
        Address newAddress = new Address();
        newAddress.setStreet("Mul HaHof Village");
        newAddress.setCity("Hadera");
        session.advanced().patch(egor, "address", newAddress);
    }

    private static void modifyEgorWithAttachmentWithLoad(IDocumentSession session) {
        Employee egor = session.load(Employee.class, EGOR_ID);
        session.advanced().attachments().store(egor, "profile-picture", imageStream());
    }

    private static void modifyEgorWithAttachmentNoLoad(IDocumentSession session) {
        session.advanced().attachments().store(EGOR_ID, "profile-picture", imageStream());
    }

    private static void modifyEgorWithTimeSeriesNoLoad(IDocumentSession session) {
        session.timeSeriesFor(EGOR_ID, "profile-picture-likes")
                .append(new Date(), new double[]{322d}, "super-like");
    }

    private static void modifyEgorWithTimeSeriesWithLoad(IDocumentSession session) {
        Employee egor = session.load(Employee.class, EGOR_ID);
        session.timeSeriesFor(egor, "profile-picture-likes")
                .append(new Date(), new double[]{322d}, "super-like");
    }

    private static void modifyEgorWithCounterWithLoad(IDocumentSession session) {
        Employee egor = session.load(Employee.class, EGOR_ID);
        session.countersFor(egor).increment("profile-picture-likes");
    }

    private static void modifyEgorWithCounterNoLoad(IDocumentSession session) {
        session.countersFor(EGOR_ID).increment("profile-picture-likes");
    }

    private static void modifyEgorWithRevisionNoLoad(IDocumentSession session) {
        session.advanced().revisions().forceRevisionCreationFor(EGOR_ID);
    }

    private static void modifyEgorWithRevisionWithLoad(IDocumentSession session) {
        Employee egor = session.load(Employee.class, EGOR_ID);
        session.advanced().revisions().forceRevisionCreationFor(egor);
    }

    // endregion

    // region helpers

    private static SessionOptions writesAndReads() {
        SessionOptions options = new SessionOptions();
        options.setOptimisticConcurrencyMode(OptimisticConcurrencyMode.WRITES_AND_READS);
        return options;
    }

    private static InMemoryDocumentSessionOperations inMem(IDocumentSession session) {
        return (InMemoryDocumentSessionOperations) session;
    }

    private static ConcurrencyException assertConcurrency(org.assertj.core.api.ThrowableAssert.ThrowingCallable callable) {
        Throwable thrown = catchThrowable(callable);
        assertThat(thrown).isInstanceOf(ConcurrencyException.class);
        return (ConcurrencyException) thrown;
    }

    private static void seedJerryAndEgor(IDocumentStore store) {
        try (IDocumentSession session = store.openSession()) {
            Employee jerry = new Employee();
            jerry.setFirstName("Jerry");
            session.store(jerry, JERRY_ID);

            Employee egor = new Employee();
            egor.setFirstName("Egor");
            egor.setAddress(street("Ahad Ha'am"));
            session.store(egor, EGOR_ID);

            session.saveChanges();
        }
    }

    private static String modifyJerryInBackground(IDocumentStore store) {
        try (IDocumentSession s = store.openSession()) {
            Employee j = s.load(Employee.class, JERRY_ID);
            Address address = new Address();
            address.setCity("Hadera");
            j.setAddress(address);
            s.saveChanges();
            return s.advanced().getChangeVectorFor(j);
        }
    }

    /**
     * Opens a second store against the same server/database with conventions customized BEFORE initialize,
     * mirroring the C# {@code ModifyDocumentStore = s => s.Conventions.X = ...} pattern.
     */
    private static DocumentStore openStoreWithConventions(IDocumentStore baseStore, OptimisticConcurrencyMode mode) {
        DocumentStore custom = new DocumentStore();
        custom.setUrls(baseStore.getUrls());
        custom.setDatabase(baseStore.getDatabase());
        custom.getConventions().setOptimisticConcurrencyMode(mode);
        custom.initialize();
        return custom;
    }

    private static ObjectNode serializeCommand(ICommandData command, DocumentConventions conventions) throws IOException {
        ObjectMapper mapper = JsonExtensions.getDefaultMapper();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (JsonGenerator generator = mapper.getFactory().createGenerator(baos)) {
            command.serialize(generator, conventions);
        }
        return (ObjectNode) mapper.readTree(baos.toByteArray());
    }

    private static DocumentInfo buildExternalDocumentInfo(String id, Object entity, String changeVector, String collection) {
        ObjectMapper mapper = JsonExtensions.getDefaultMapper();
        ObjectNode metadata = mapper.createObjectNode();
        metadata.put(Constants.Documents.Metadata.COLLECTION, collection);
        metadata.put(Constants.Documents.Metadata.CHANGE_VECTOR, changeVector);

        DocumentInfo documentInfo = new DocumentInfo();
        documentInfo.setId(id);
        documentInfo.setEntity(entity);
        documentInfo.setChangeVector(changeVector);
        documentInfo.setDocument(null);
        documentInfo.setMetadata(metadata);
        return documentInfo;
    }

    // endregion

    @ParameterizedTest
    @MethodSource("sessionActions")
    public void shouldThrowConcurrencyException_WhenTrackedEntityWasChangedInBackgroundSession(NamedAction sessionAction) throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            seedJerryAndEgor(store);

            try (IDocumentSession session = store.openSession(writesAndReads())) {
                Employee jerry = session.load(Employee.class, JERRY_ID);

                sessionAction.invoke(session);

                String expected = session.advanced().getChangeVectorFor(jerry);
                assertThat(expected).isNotEmpty();

                String actual = modifyJerryInBackground(store);
                assertThat(actual).isNotEmpty();

                ConcurrencyException e = assertConcurrency(session::saveChanges);

                assertThat(e.getMessage()).contains("Document 'employees/1-A' has been modified");
                assertThat(e.getId()).isEqualTo(JERRY_ID);
                assertThat(e.getActualChangeVector()).isEqualTo(actual);
                assertThat(e.getExpectedChangeVector()).isEqualTo(expected);
            }

            try (IDocumentSession session = store.openSession(writesAndReads())) {
                Employee egor = session.load(Employee.class, EGOR_ID);
                assertThat(egor.getFirstName()).isEqualTo("Egor");
                assertThat(egor.getAddress().getStreet()).isEqualTo("Ahad Ha'am");

                Employee j = session.load(Employee.class, JERRY_ID);
                assertThat(j.getAddress().getCity()).isEqualTo("Hadera");
            }
        }
    }

    @Test
    public void shouldThrowConcurrencyException_WhenNoCommandsInSessionButTrackedEntityWasChangedInBackgroundSession() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            seedJerryAndEgor(store);

            try (IDocumentSession session = store.openSession(writesAndReads())) {
                Employee jerry = session.load(Employee.class, JERRY_ID);

                modifyEgorInSession(session);

                String expected = session.advanced().getChangeVectorFor(jerry);
                assertThat(expected).isNotEmpty();

                session.saveChanges();

                String actual = modifyJerryInBackground(store);
                assertThat(actual).isNotEmpty();

                ConcurrencyException e = assertConcurrency(session::saveChanges);

                assertThat(e.getMessage()).contains("Document 'employees/1-A' has been modified");
                assertThat(e.getId()).isEqualTo(JERRY_ID);
                assertThat(e.getActualChangeVector()).isEqualTo(actual);
                assertThat(e.getExpectedChangeVector()).isEqualTo(expected);
            }

            try (IDocumentSession session = store.openSession(writesAndReads())) {
                Employee egor = session.load(Employee.class, EGOR_ID);
                assertThat(egor.getFirstName()).isEqualTo("Egor");
                assertThat(egor.getAddress().getStreet()).isEqualTo("Mul HaHof Village");

                Employee j = session.load(Employee.class, JERRY_ID);
                assertThat(j.getAddress().getCity()).isEqualTo("Hadera");
            }
        }
    }

    @Test
    public void shouldThrowConcurrencyException_WhenTrackedEntityIsNullButThenWasAddedInBackgroundSession() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                Employee egor = new Employee();
                egor.setFirstName("Egor");
                egor.setAddress(street("Ahad Ha'am"));
                session.store(egor, EGOR_ID);
                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession(writesAndReads())) {
                Employee jerry = session.load(Employee.class, JERRY_ID);
                assertThat(jerry).isNull();

                modifyEgorInSession(session);

                String actual;
                try (IDocumentSession s = store.openSession()) {
                    Employee j = new Employee();
                    j.setFirstName("Jerry");
                    Address address = new Address();
                    address.setCity("Hadera");
                    j.setAddress(address);
                    s.store(j, JERRY_ID);
                    s.saveChanges();
                    actual = s.advanced().getChangeVectorFor(j);
                }

                assertThat(actual).isNotEmpty();

                ConcurrencyException e = assertConcurrency(session::saveChanges);

                assertThat(e.getMessage()).contains("Document 'employees/1-A' has been modified");
                assertThat(e.getId()).isEqualTo(JERRY_ID);
                assertThat(e.getActualChangeVector()).isEqualTo(actual);
                assertThat(e.getExpectedChangeVector()).isNullOrEmpty();
            }

            try (IDocumentSession session = store.openSession(writesAndReads())) {
                Employee egor = session.load(Employee.class, EGOR_ID);
                assertThat(egor.getFirstName()).isEqualTo("Egor");
                assertThat(egor.getAddress().getStreet()).isEqualTo("Ahad Ha'am");

                Employee j = session.load(Employee.class, JERRY_ID);
                assertThat(j.getAddress().getCity()).isEqualTo("Hadera");
            }
        }
    }

    @Test
    public void shouldThrowConcurrencyException_WhenTrackedEntityDeletedByEntityButThenWasEditedInBackgroundSession() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            seedJerryAndEgor(store);

            try (IDocumentSession session = store.openSession(writesAndReads())) {
                Employee jerry = session.load(Employee.class, JERRY_ID);
                session.delete(jerry);
                modifyEgorInSession(session);

                String expected = session.advanced().getChangeVectorFor(jerry);
                assertThat(expected).isNotEmpty();

                String actual = modifyJerryInBackground(store);
                assertThat(actual).isNotEmpty();

                // this should throw concurrency exception for jerry via the DELETE command's own CV check
                ConcurrencyException e = assertConcurrency(session::saveChanges);

                assertThat(e.getMessage())
                        .contains("Document employees/1-A has change vector " + actual + ", but Delete was called with change vector '" + expected + "'");
                assertThat(e.getId()).isEqualTo(JERRY_ID);
                assertThat(e.getActualChangeVector()).isEqualTo(actual);
                assertThat(e.getExpectedChangeVector()).isEqualTo(expected);
            }

            try (IDocumentSession session = store.openSession(writesAndReads())) {
                Employee egor = session.load(Employee.class, EGOR_ID);
                assertThat(egor.getFirstName()).isEqualTo("Egor");
                assertThat(egor.getAddress().getStreet()).isEqualTo("Ahad Ha'am");

                Employee j = session.load(Employee.class, JERRY_ID);
                assertThat(j.getAddress().getCity()).isEqualTo("Hadera");
            }
        }
    }

    @Test
    public void shouldNotThrowConcurrencyException_WhenTrackedEntityDeletedByIdWithoutChangeVectorButThenWasNotEditedInBackgroundSession() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            seedJerryAndEgor(store);

            try (IDocumentSession session = store.openSession(writesAndReads())) {
                session.load(Employee.class, JERRY_ID);
                session.delete(JERRY_ID);
                modifyEgorInSession(session);

                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession(writesAndReads())) {
                Employee egor = session.load(Employee.class, EGOR_ID);
                assertThat(egor.getFirstName()).isEqualTo("Egor");
                assertThat(egor.getAddress().getStreet()).isEqualTo("Mul HaHof Village");

                Employee j = session.load(Employee.class, JERRY_ID);
                assertThat(j).isNull();
            }
        }
    }

    @Test
    public void shouldNotThrowConcurrencyException_WhenTrackedEntityDeletedByIdWithChangeVectorButThenWasNotEditedInBackgroundSession2() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            seedJerryAndEgor(store);

            try (IDocumentSession session = store.openSession(writesAndReads())) {
                Employee jerry = session.load(Employee.class, JERRY_ID);
                String actual = session.advanced().getChangeVectorFor(jerry);
                session.delete(JERRY_ID, "test");
                modifyEgorInSession(session);

                ConcurrencyException e = assertConcurrency(session::saveChanges);

                assertThat(e.getMessage())
                        .contains("Document " + e.getId() + " has change vector " + e.getActualChangeVector()
                                + ", but Delete was called with change vector '" + e.getExpectedChangeVector()
                                + "'. Optimistic concurrency violation, transaction will be aborted.");

                assertThat(e.getId()).isEqualTo(JERRY_ID);
                assertThat(e.getActualChangeVector()).isEqualTo(actual);
                assertThat(e.getExpectedChangeVector()).isEqualTo("test");
            }

            try (IDocumentSession session = store.openSession(writesAndReads())) {
                Employee egor = session.load(Employee.class, EGOR_ID);
                assertThat(egor.getFirstName()).isEqualTo("Egor");
                assertThat(egor.getAddress().getStreet()).isEqualTo("Ahad Ha'am");

                Employee j = session.load(Employee.class, JERRY_ID);
                assertThat(j.getFirstName()).isEqualTo("Jerry");
            }
        }
    }

    @Test
    public void shouldThrowConcurrencyException_WhenTrackedEntityDeletedByIdButThenWasEditedInBackgroundSession() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            seedJerryAndEgor(store);

            try (IDocumentSession session = store.openSession(writesAndReads())) {
                Employee jerry = session.load(Employee.class, JERRY_ID);
                String expected = session.advanced().getChangeVectorFor(jerry);
                session.delete(JERRY_ID);
                modifyEgorInSession(session);

                String actual = modifyJerryInBackground(store);
                assertThat(actual).isNotEmpty();

                ConcurrencyException e = assertConcurrency(session::saveChanges);

                assertThat(e.getMessage()).contains("Document 'employees/1-A' has been modified");
                assertThat(e.getId()).isEqualTo(JERRY_ID);
                assertThat(e.getActualChangeVector()).isEqualTo(actual);
                assertThat(e.getExpectedChangeVector()).isEqualTo(expected);
            }

            try (IDocumentSession session = store.openSession(writesAndReads())) {
                Employee egor = session.load(Employee.class, EGOR_ID);
                assertThat(egor.getFirstName()).isEqualTo("Egor");
                assertThat(egor.getAddress().getStreet()).isEqualTo("Ahad Ha'am");

                Employee j = session.load(Employee.class, JERRY_ID);
                assertThat(j.getAddress().getCity()).isEqualTo("Hadera");
            }
        }
    }

    @ParameterizedTest
    @MethodSource("sessionActions")
    public void shouldThrowConcurrencyException_WhenTrackedEntityIncludedBySessionButThenWasEditedInBackgroundSession(NamedAction sessionAction) throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            String addressId;
            try (IDocumentSession session = store.openSession()) {
                Address address = new Address();
                address.setCity("Harish");
                address.setStreet("Erets Rd");
                session.store(address);
                addressId = session.advanced().getDocumentId(address);
                assertThat(addressId).isNotNull();
                address.setId(addressId);

                Employee jerry = new Employee();
                jerry.setFirstName("Jerry");
                jerry.setAddress(address);
                session.store(jerry, JERRY_ID);

                Employee egor = new Employee();
                egor.setFirstName("Egor");
                egor.setAddress(street("Ahad Ha'am"));
                session.store(egor, EGOR_ID);

                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession(writesAndReads())) {
                Employee jerry = session.include("address.id").load(Employee.class, JERRY_ID);

                int numOfRequests = session.advanced().getNumberOfRequests();

                Address address = session.load(Address.class, jerry.getAddress().getId());
                assertThat(address).isNotNull();
                assertThat(session.advanced().getNumberOfRequests()).isEqualTo(numOfRequests);

                sessionAction.invoke(session);

                String expected = session.advanced().getChangeVectorFor(address);
                assertThat(expected).isNotEmpty();

                String actual;
                try (IDocumentSession s = store.openSession()) {
                    Address a = s.load(Address.class, addressId);
                    a.setCity("Hadera");
                    s.saveChanges();
                    actual = s.advanced().getChangeVectorFor(a);
                }

                assertThat(actual).isNotEmpty();

                ConcurrencyException e = assertConcurrency(session::saveChanges);

                assertThat(e.getMessage()).contains("Document '" + addressId + "' has been modified");
                assertThat(e.getId()).isEqualTo(addressId);
                assertThat(e.getActualChangeVector()).isEqualTo(actual);
                assertThat(e.getExpectedChangeVector()).isEqualTo(expected);
            }

            try (IDocumentSession session = store.openSession(writesAndReads())) {
                Employee egor = session.load(Employee.class, EGOR_ID);
                assertThat(egor.getFirstName()).isEqualTo("Egor");
                assertThat(egor.getAddress().getStreet()).isEqualTo("Ahad Ha'am");

                Address j = session.load(Address.class, addressId);
                assertThat(j.getCity()).isEqualTo("Hadera");
            }
        }
    }

    @Test
    public void shouldThrowConcurrencyException_WhenTrackedEntityEvictedFromTheSessionAndThenAddedBack() throws Exception {
        // WritesAndReads mode includes write concurrency checks.
        // After evicting and re-storing with the same ID, the entity is treated as new (empty CV),
        // so the server rejects the put because the document already exists.
        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                Address address = new Address();
                address.setCity("Harish");
                address.setStreet("Erets Rd");
                session.store(address);

                Employee jerry = new Employee();
                jerry.setFirstName("Jerry");
                jerry.setAddress(address);
                session.store(jerry, JERRY_ID);

                Employee egor = new Employee();
                egor.setFirstName("Egor");
                egor.setAddress(street("Ahad Ha'am"));
                session.store(egor, EGOR_ID);

                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession(writesAndReads())) {
                Employee jerry = session.include("address.id").load(Employee.class, JERRY_ID);
                Address address = session.load(Address.class, jerry.getAddress().getId());
                assertThat(address).isNotNull();

                modifyEgorWithStoreOverwriteAfterLoadAndEvict(session);

                assertThatThrownBy(session::saveChanges).isInstanceOf(ConcurrencyException.class);
            }
        }
    }

    @Test
    public void shouldThrowConcurrencyException_WhenTrackedEntityReStored() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                Employee egor = new Employee();
                egor.setFirstName("Egor");
                egor.setAddress(street("Ahad Ha'am"));
                session.store(egor, EGOR_ID);
                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession(writesAndReads())) {
                Employee egor = new Employee();
                egor.setFirstName("Egor");
                egor.setAddress(street("Mul HaHof Village"));
                session.store(egor, EGOR_ID);

                assertThatThrownBy(session::saveChanges).isInstanceOf(ConcurrencyException.class);
            }
        }
    }

    @Test
    public void shouldThrowConcurrencyException_WhenNonExistsEntityIncludedBySessionButThenWasEditedInBackgroundSession() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            String addressId = "addresses/1-A";

            try (IDocumentSession session = store.openSession()) {
                Address address = new Address();
                address.setCity("Harish");
                address.setStreet("Erets Rd");
                address.setId(addressId);
                session.store(address);
                assertThat(session.advanced().getDocumentId(address)).isEqualTo(addressId);

                Employee jerry = new Employee();
                jerry.setFirstName("Jerry");
                jerry.setAddress(address);
                session.store(jerry, JERRY_ID);

                Employee egor = new Employee();
                egor.setFirstName("Egor");
                egor.setAddress(street("Ahad Ha'am"));
                session.store(egor, EGOR_ID);

                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession(writesAndReads())) {
                Employee jerry = session.include("address.id").load(Employee.class, JERRY_ID);

                int numOfRequests = session.advanced().getNumberOfRequests();
                Address adr = session.load(Address.class, jerry.getAddress().getId());
                assertThat(adr).isNotNull();
                assertThat(session.advanced().getNumberOfRequests()).isEqualTo(numOfRequests);

                modifyEgorInSession(session);

                String expected = session.advanced().getChangeVectorFor(adr);
                assertThat(expected).isNotEmpty();

                String actual;
                try (IDocumentSession s = store.openSession()) {
                    Address adrInternal = s.load(Address.class, addressId);
                    adrInternal.setCity("Hadera");
                    s.saveChanges();
                    actual = s.advanced().getChangeVectorFor(adrInternal);
                }

                assertThat(actual).isNotEmpty();

                ConcurrencyException e = assertConcurrency(session::saveChanges);

                assertThat(e.getMessage()).contains("Document 'addresses/1-A' has been modified");
                assertThat(e.getId()).isEqualTo(addressId);
                assertThat(e.getActualChangeVector()).isEqualTo(actual);
                assertThat(e.getExpectedChangeVector()).isEqualTo(expected);
            }

            try (IDocumentSession session = store.openSession(writesAndReads())) {
                Employee egor = session.load(Employee.class, EGOR_ID);
                assertThat(egor.getFirstName()).isEqualTo("Egor");
                assertThat(egor.getAddress().getStreet()).isEqualTo("Ahad Ha'am");

                Address j = session.load(Address.class, addressId);
                assertThat(j).isNotNull();
                assertThat(j.getCity()).isEqualTo("Hadera");
            }
        }
    }

    @Test
    public void shouldThrowConcurrencyException_WhenEntityIncludedByIdInSessionButThenWasEditedInBackgroundSession() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            String addressId;
            try (IDocumentSession session = store.openSession()) {
                Address address = new Address();
                address.setCity("Harish");
                address.setStreet("Erets Rd");
                session.store(address);
                addressId = session.advanced().getDocumentId(address);
                assertThat(addressId).isNotNull();
                address.setId(addressId);

                Employee jerry = new Employee();
                jerry.setFirstName("Jerry");
                jerry.setAddress(address);
                session.store(jerry, JERRY_ID);

                Employee egor = new Employee();
                egor.setFirstName("Egor");
                egor.setAddress(street("Ahad Ha'am"));
                session.store(egor, EGOR_ID);

                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession(writesAndReads())) {
                session.include("address.id").load(Employee.class, JERRY_ID);
                modifyEgorInSession(session);

                DocumentInfo included = inMem(session).includedDocumentsById.get(addressId);
                assertThat(included).isNotNull();
                String expected = included.getChangeVector();
                assertThat(expected).isNotEmpty();

                String actual;
                try (IDocumentSession s = store.openSession()) {
                    Address a = s.load(Address.class, addressId);
                    a.setCity("Hadera");
                    s.saveChanges();
                    actual = s.advanced().getChangeVectorFor(a);
                }

                assertThat(actual).isNotEmpty();

                ConcurrencyException e = assertConcurrency(session::saveChanges);

                assertThat(e.getMessage()).contains("Document 'addresses/1-A' has been modified");
                assertThat(e.getId()).isEqualTo(addressId);
                assertThat(e.getActualChangeVector()).isEqualTo(actual);
                assertThat(e.getExpectedChangeVector()).isEqualTo(expected);
            }

            try (IDocumentSession session = store.openSession(writesAndReads())) {
                Employee egor = session.load(Employee.class, EGOR_ID);
                assertThat(egor.getFirstName()).isEqualTo("Egor");
                assertThat(egor.getAddress().getStreet()).isEqualTo("Ahad Ha'am");

                Address j = session.load(Address.class, addressId);
                assertThat(j.getCity()).isEqualTo("Hadera");
            }
        }
    }

    @Test
    public void shouldNotThrowConcurrencyException_WhenTrackedEntityEvictedFromSession() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            seedJerryAndEgor(store);

            try (IDocumentSession session = store.openSession(writesAndReads())) {
                Employee jerry = session.load(Employee.class, JERRY_ID);
                String expected = session.advanced().getChangeVectorFor(jerry);

                modifyEgorInSession(session);

                // Evict Jerry from the session - this should stop tracking him
                session.advanced().evict(jerry);

                String actual = modifyJerryInBackground(store);
                assertThat(actual).isNotEmpty();
                assertThat(actual).isNotEqualTo(expected);

                // Should NOT throw concurrency exception because Jerry was evicted
                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession(writesAndReads())) {
                Employee egor = session.load(Employee.class, EGOR_ID);
                assertThat(egor.getFirstName()).isEqualTo("Egor");
                assertThat(egor.getAddress().getStreet()).isEqualTo("Mul HaHof Village");

                Employee j = session.load(Employee.class, JERRY_ID);
                assertThat(j.getAddress().getCity()).isEqualTo("Hadera");
            }
        }
    }

    @Test
    public void shouldNotThrowConcurrencyException_WhenIncludedDocumentEvictedFromSession() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            String addressId;
            try (IDocumentSession session = store.openSession(writesAndReads())) {
                Address address = new Address();
                address.setCity("Harish");
                address.setStreet("Erets Rd");
                session.store(address);
                addressId = session.advanced().getDocumentId(address);
                assertThat(addressId).isNotNull();
                address.setId(addressId);

                Employee jerry = new Employee();
                jerry.setFirstName("Jerry");
                jerry.setAddress(address);
                session.store(jerry, JERRY_ID);

                Employee egor = new Employee();
                egor.setFirstName("Egor");
                egor.setAddress(street("Ahad Ha'am"));
                session.store(egor, EGOR_ID);

                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession(writesAndReads())) {
                Employee jerry = session.include("address.id").load(Employee.class, JERRY_ID);

                int numOfRequests = session.advanced().getNumberOfRequests();
                Address address = session.load(Address.class, jerry.getAddress().getId());
                assertThat(address).isNotNull();
                assertThat(session.advanced().getNumberOfRequests()).isEqualTo(numOfRequests);

                String expected = session.advanced().getChangeVectorFor(address);

                // Evict the included address from tracking
                session.advanced().evict(address);

                modifyEgorInSession(session);

                String actual;
                try (IDocumentSession s = store.openSession()) {
                    Address a = s.load(Address.class, addressId);
                    a.setCity("Hadera");
                    s.saveChanges();
                    actual = s.advanced().getChangeVectorFor(a);
                }

                assertThat(actual).isNotEmpty();
                assertThat(actual).isNotEqualTo(expected);

                // Should NOT throw concurrency exception because address was evicted
                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession(writesAndReads())) {
                Employee egor = session.load(Employee.class, EGOR_ID);
                assertThat(egor.getFirstName()).isEqualTo("Egor");
                assertThat(egor.getAddress().getStreet()).isEqualTo("Mul HaHof Village");

                Address j = session.load(Address.class, addressId);
                assertThat(j.getCity()).isEqualTo("Hadera");
            }
        }
    }

    @Test
    public void shouldNotThrowConcurrencyException_WhenEntityModifiedInBackgroundSessionButThenRefreshed() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                Employee jerry = new Employee();
                jerry.setFirstName("Jerry");
                Address address = new Address();
                address.setCity("Hadera");
                jerry.setAddress(address);
                session.store(jerry, JERRY_ID);

                Employee egor = new Employee();
                egor.setFirstName("Egor");
                egor.setAddress(street("Ahad Ha'am"));
                session.store(egor, EGOR_ID);

                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession(writesAndReads())) {
                Employee jerry = session.load(Employee.class, JERRY_ID);
                session.load(Employee.class, EGOR_ID);

                try (IDocumentSession s = store.openSession()) {
                    Employee j = s.load(Employee.class, JERRY_ID);
                    Address address = new Address();
                    address.setCity("Tel Aviv");
                    j.setAddress(address);
                    s.saveChanges();
                }

                session.advanced().refresh(jerry);
                assertThat(jerry.getAddress().getCity()).isEqualTo("Tel Aviv");

                modifyEgorInSession(session);

                // should work since we refreshed jerry after modification in background session
                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession(writesAndReads())) {
                Employee egor = session.load(Employee.class, EGOR_ID);
                assertThat(egor.getFirstName()).isEqualTo("Egor");
                assertThat(egor.getAddress().getStreet()).isEqualTo("Mul HaHof Village");

                Employee j = session.load(Employee.class, JERRY_ID);
                assertThat(j.getFirstName()).isEqualTo("Jerry");
                assertThat(j.getAddress().getCity()).isEqualTo("Tel Aviv");
            }
        }
    }

    @Test
    public void shouldThrowConcurrencyException_WhenRefreshedEntityModifiedInBackgroundSession() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                Employee jerry = new Employee();
                jerry.setFirstName("Jerry");
                Address address = new Address();
                address.setCity("Hadera");
                jerry.setAddress(address);
                session.store(jerry, JERRY_ID);

                Employee egor = new Employee();
                egor.setFirstName("Egor");
                egor.setAddress(street("Ahad Ha'am"));
                session.store(egor, EGOR_ID);

                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession(writesAndReads())) {
                Employee jerry = session.load(Employee.class, JERRY_ID);
                session.load(Employee.class, EGOR_ID);

                try (IDocumentSession s = store.openSession()) {
                    Employee j = s.load(Employee.class, JERRY_ID);
                    Address address = new Address();
                    address.setCity("Tel Aviv");
                    j.setAddress(address);
                    s.saveChanges();
                }

                session.advanced().refresh(jerry);
                assertThat(jerry.getAddress().getCity()).isEqualTo("Tel Aviv");

                String expectedJerryCv = session.advanced().getChangeVectorFor(jerry);

                modifyEgorInSession(session);

                String actualJerryCv;
                try (IDocumentSession s = store.openSession()) {
                    Employee j = s.load(Employee.class, JERRY_ID);
                    j.setFirstName("Jeremy");
                    s.saveChanges();
                    actualJerryCv = s.advanced().getChangeVectorFor(j);
                }

                assertThat(actualJerryCv).isNotEmpty();
                assertThat(actualJerryCv).isNotEqualTo(expectedJerryCv);

                ConcurrencyException e = assertConcurrency(session::saveChanges);

                assertThat(e.getMessage()).contains("Document 'employees/1-A' has been modified");
                assertThat(e.getId()).isEqualTo(JERRY_ID);
                assertThat(e.getActualChangeVector()).isEqualTo(actualJerryCv);
                assertThat(e.getExpectedChangeVector()).isEqualTo(expectedJerryCv);
            }

            try (IDocumentSession session = store.openSession(writesAndReads())) {
                Employee egor = session.load(Employee.class, EGOR_ID);
                assertThat(egor.getFirstName()).isEqualTo("Egor");
                assertThat(egor.getAddress().getStreet()).isEqualTo("Ahad Ha'am");

                Employee j = session.load(Employee.class, JERRY_ID);
                assertThat(j.getFirstName()).isEqualTo("Jeremy");
                assertThat(j.getAddress().getCity()).isEqualTo("Tel Aviv");
            }
        }
    }

    @Test
    public void shouldUpdateChangeVector_WhenRefreshingMultipleEntities() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                Employee jerry = new Employee();
                jerry.setFirstName("Jerry");
                session.store(jerry, JERRY_ID);

                Employee egor = new Employee();
                egor.setFirstName("Egor");
                session.store(egor, EGOR_ID);

                Employee alice = new Employee();
                alice.setFirstName("Alice");
                session.store(alice, "employees/3-A");

                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession(writesAndReads())) {
                Employee jerry = session.load(Employee.class, JERRY_ID);
                Employee egor = session.load(Employee.class, EGOR_ID);
                Employee alice = session.load(Employee.class, "employees/3-A");

                String originalJerryCv = session.advanced().getChangeVectorFor(jerry);
                String originalEgorCv = session.advanced().getChangeVectorFor(egor);
                String originalAliceCv = session.advanced().getChangeVectorFor(alice);

                try (IDocumentSession s = store.openSession()) {
                    Employee j = s.load(Employee.class, JERRY_ID);
                    j.setFirstName("Jeremy");

                    Employee e = s.load(Employee.class, EGOR_ID);
                    e.setFirstName("Greg");

                    s.saveChanges();
                }

                session.advanced().refresh(Arrays.asList(jerry, egor, alice));

                String newJerryCv = session.advanced().getChangeVectorFor(jerry);
                String newEgorCv = session.advanced().getChangeVectorFor(egor);
                String newAliceCv = session.advanced().getChangeVectorFor(alice);

                assertThat(newJerryCv).isNotEqualTo(originalJerryCv);
                assertThat(newEgorCv).isNotEqualTo(originalEgorCv);
                assertThat(newAliceCv).isEqualTo(originalAliceCv); // Alice wasn't modified

                assertThat(jerry.getFirstName()).isEqualTo("Jeremy");
                assertThat(egor.getFirstName()).isEqualTo("Greg");
                assertThat(alice.getFirstName()).isEqualTo("Alice");

                session.saveChanges(); // Should not throw
            }
        }
    }

    @Test
    public void shouldThrowException_WhenEvictingEntityDuringOnBeforeStore() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                Employee jerry = new Employee();
                jerry.setFirstName("Jerry");
                session.store(jerry, JERRY_ID);
                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession(writesAndReads())) {
                Employee jerry = session.load(Employee.class, JERRY_ID);

                session.advanced().addBeforeStoreListener((sender, args) ->
                        // This should throw because we can't evict during OnBeforeStore
                        assertThatThrownBy(() -> session.advanced().evict(args.getEntity()))
                                .isInstanceOf(IllegalStateException.class));

                jerry.setFirstName("Jeremy");
                session.saveChanges();
            }
        }
    }

    @Test
    public void shouldHandleEvictAndReload_WithTrackingEnabled() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                Employee jerry = new Employee();
                jerry.setFirstName("Jerry");
                Address address = new Address();
                address.setCity("Hadera");
                jerry.setAddress(address);
                session.store(jerry, JERRY_ID);
                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession(writesAndReads())) {
                Employee jerry = session.load(Employee.class, JERRY_ID);
                String originalCv = session.advanced().getChangeVectorFor(jerry);

                try (IDocumentSession s = store.openSession()) {
                    Employee j = s.load(Employee.class, JERRY_ID);
                    j.getAddress().setCity("Tel Aviv");
                    s.saveChanges();
                }

                session.advanced().evict(jerry);

                Employee jerryReloaded = session.load(Employee.class, JERRY_ID);
                String newCv = session.advanced().getChangeVectorFor(jerryReloaded);

                assertThat(jerryReloaded).isNotSameAs(jerry);
                assertThat(newCv).isNotEqualTo(originalCv);
                assertThat(jerryReloaded.getAddress().getCity()).isEqualTo("Tel Aviv");

                try (IDocumentSession s = store.openSession()) {
                    Employee j = s.load(Employee.class, JERRY_ID);
                    j.setFirstName("Jeremy");
                    s.saveChanges();
                }

                ConcurrencyException e = assertConcurrency(session::saveChanges);

                assertThat(e.getMessage()).contains("Document 'employees/1-A' has been modified");
                assertThat(e.getId()).isEqualTo(JERRY_ID);
            }
        }
    }

    @Test
    public void shouldHandleEvictAndReload_WithTrackingEnabled2() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                Employee jerry = new Employee();
                jerry.setFirstName("Jerry");
                Address address = new Address();
                address.setCity("Hadera");
                jerry.setAddress(address);
                session.store(jerry, JERRY_ID);

                Employee egor = new Employee();
                egor.setFirstName("Egor");
                egor.setAddress(street("Ahad Ha'am"));
                session.store(egor, EGOR_ID);

                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession(writesAndReads())) {
                Employee jerry = session.load(Employee.class, JERRY_ID);
                modifyEgorInSession(session);

                try (IDocumentSession s = store.openSession()) {
                    Employee j = s.load(Employee.class, JERRY_ID);
                    j.getAddress().setCity("Tel Aviv");
                    s.saveChanges();
                }

                session.advanced().evict(jerry);

                // Should not throw because the entity was evicted
                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession(writesAndReads())) {
                Employee egor = session.load(Employee.class, EGOR_ID);
                assertThat(egor.getFirstName()).isEqualTo("Egor");
                assertThat(egor.getAddress().getStreet()).isEqualTo("Mul HaHof Village");

                Employee j = session.load(Employee.class, JERRY_ID);
                assertThat(j.getFirstName()).isEqualTo("Jerry");
                assertThat(j.getAddress().getCity()).isEqualTo("Tel Aviv");
            }
        }
    }

    @Test
    public void shouldHandleEvictAndReload_WithTrackingEnabled3() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                Employee jerry = new Employee();
                jerry.setFirstName("Jerry");
                Address address = new Address();
                address.setCity("Hadera");
                jerry.setAddress(address);
                session.store(jerry, JERRY_ID);

                Employee egor = new Employee();
                egor.setFirstName("Egor");
                egor.setAddress(street("Ahad Ha'am"));
                session.store(egor, EGOR_ID);

                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession(writesAndReads())) {
                Employee jerry = session.load(Employee.class, JERRY_ID);
                modifyEgorInSession(session);

                session.advanced().evict(jerry);

                try (IDocumentSession s = store.openSession()) {
                    Employee j = s.load(Employee.class, JERRY_ID);
                    j.getAddress().setCity("Tel Aviv");
                    s.saveChanges();
                }

                // Should not throw because the entity was evicted
                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession(writesAndReads())) {
                Employee egor = session.load(Employee.class, EGOR_ID);
                assertThat(egor.getFirstName()).isEqualTo("Egor");
                assertThat(egor.getAddress().getStreet()).isEqualTo("Mul HaHof Village");

                Employee j = session.load(Employee.class, JERRY_ID);
                assertThat(j.getFirstName()).isEqualTo("Jerry");
                assertThat(j.getAddress().getCity()).isEqualTo("Tel Aviv");
            }
        }
    }

    @Test
    public void shouldNotTrack_AfterEvictingAllLoadedEntities() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                Employee jerry = new Employee();
                jerry.setFirstName("Jerry");
                session.store(jerry, JERRY_ID);

                Employee egor = new Employee();
                egor.setFirstName("Egor");
                session.store(egor, EGOR_ID);

                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession(writesAndReads())) {
                Employee jerry = session.load(Employee.class, JERRY_ID);
                Employee egor = session.load(Employee.class, EGOR_ID);

                assertThat(inMem(session).getNumberOfEntitiesInUnitOfWork()).isEqualTo(2);

                session.advanced().evict(jerry);
                session.advanced().evict(egor);

                assertThat(inMem(session).getNumberOfEntitiesInUnitOfWork()).isEqualTo(0);

                try (IDocumentSession s = store.openSession()) {
                    Employee j = s.load(Employee.class, JERRY_ID);
                    j.setFirstName("Jeremy");

                    Employee e = s.load(Employee.class, EGOR_ID);
                    e.setFirstName("Greg");

                    s.saveChanges();
                }

                // Should not throw because entities were evicted
                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                Employee jerry = session.load(Employee.class, JERRY_ID);
                Employee egor = session.load(Employee.class, EGOR_ID);

                assertThat(jerry.getFirstName()).isEqualTo("Jeremy");
                assertThat(egor.getFirstName()).isEqualTo("Greg");
            }
        }
    }

    @Test
    public void shouldRefreshAndMaintainTracking_WhenEntityModifiedLocally() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                Employee jerry = new Employee();
                jerry.setFirstName("Jerry");
                Address address = new Address();
                address.setCity("Hadera");
                jerry.setAddress(address);
                session.store(jerry, JERRY_ID);
                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession(writesAndReads())) {
                Employee jerry = session.load(Employee.class, JERRY_ID);

                jerry.getAddress().setStreet("Local Street");

                session.advanced().refresh(jerry);

                // Local changes should be lost
                assertThat(jerry.getAddress().getStreet()).isNull();
                assertThat(jerry.getAddress().getCity()).isEqualTo("Hadera");

                assertThat(session.advanced().isLoaded(JERRY_ID)).isTrue();

                jerry.setFirstName("Jeremy");
                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                Employee jerry = session.load(Employee.class, JERRY_ID);
                assertThat(jerry.getFirstName()).isEqualTo("Jeremy");
            }
        }
    }

    @Test
    public void shouldTrackExternalEntity_WhenAddedViaTrackEntityMethod() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            seedJerryAndEgor(store);

            try (IDocumentSession session = store.openSession(writesAndReads())) {
                session.load(Employee.class, JERRY_ID);

                Employee externalEmployee;
                String externalChangeVector;
                try (IDocumentSession externalSession = store.openSession()) {
                    externalEmployee = externalSession.load(Employee.class, EGOR_ID);
                    externalChangeVector = externalSession.advanced().getChangeVectorFor(externalEmployee);
                }

                InMemoryDocumentSessionOperations inMemSession = inMem(session);
                DocumentInfo documentInfo = buildExternalDocumentInfo(EGOR_ID, externalEmployee, externalChangeVector, "Employees");

                inMemSession.registerExternalLoadedIntoTheSession(documentInfo);

                Reference<String> trackedCv = new Reference<>();
                assertThat(inMemSession.getTrackedEntitiesHolder().tryGetValue(EGOR_ID, trackedCv)).isTrue();
                assertThat(trackedCv.value).isEqualTo(externalChangeVector);

                String actualEgorCv;
                try (IDocumentSession s = store.openSession()) {
                    Employee e = s.load(Employee.class, EGOR_ID);
                    e.setAddress(street("Mul HaHof Village"));
                    s.saveChanges();
                    actualEgorCv = s.advanced().getChangeVectorFor(e);
                }

                // external entities have Document=null, so EntityChanged returns true and a PUT is generated.
                // The concurrency error comes from the PUT command, not the BatchTrackChangesCommand.
                ConcurrencyException ex = assertConcurrency(session::saveChanges);

                assertThat(ex.getMessage())
                        .contains("Document employees/2-A has change vector " + actualEgorCv + ", but Put was called with change vector " + externalChangeVector);
                assertThat(ex.getId()).isEqualTo(EGOR_ID);
                assertThat(ex.getActualChangeVector()).isEqualTo(actualEgorCv);
                assertThat(ex.getExpectedChangeVector()).isEqualTo(externalChangeVector);
            }
        }
    }

    @Test
    public void shouldNotThrowConcurrencyException_WhenExternallyTrackedEntityNotModified() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            seedJerryAndEgor(store);

            try (IDocumentSession session = store.openSession(writesAndReads())) {
                session.load(Employee.class, JERRY_ID);

                Employee externalEmployee;
                String externalChangeVector;
                try (IDocumentSession externalSession = store.openSession()) {
                    externalEmployee = externalSession.load(Employee.class, EGOR_ID);
                    externalChangeVector = externalSession.advanced().getChangeVectorFor(externalEmployee);
                }

                InMemoryDocumentSessionOperations inMemSession = inMem(session);
                DocumentInfo documentInfo = buildExternalDocumentInfo(EGOR_ID, externalEmployee, externalChangeVector, "Employees");

                documentInfo.setDocument(inMemSession.getEntityToJson().convertEntityToJson(externalEmployee, documentInfo));
                inMemSession.trackEntity(Employee.class, documentInfo);

                try (IDocumentSession s = store.openSession()) {
                    Employee j = s.load(Employee.class, JERRY_ID);
                    Address address = new Address();
                    address.setCity("Hadera");
                    j.setAddress(address);
                    s.saveChanges();
                }

                ConcurrencyException ex = assertConcurrency(session::saveChanges);

                assertThat(ex.getMessage()).contains("Document 'employees/1-A' has been modified");
                assertThat(ex.getId()).isEqualTo(JERRY_ID);
            }

            try (IDocumentSession session = store.openSession()) {
                Employee jerry = session.load(Employee.class, JERRY_ID);
                Employee egor = session.load(Employee.class, EGOR_ID);

                assertThat(jerry.getAddress().getCity()).isEqualTo("Hadera");
                assertThat(egor.getAddress().getStreet()).isEqualTo("Ahad Ha'am");
            }
        }
    }

    @Test
    public void shouldThrowException_WhenRegisteringExternalEntityWithDifferentInstanceForSameId() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                Employee jerry = new Employee();
                jerry.setFirstName("Jerry");
                session.store(jerry, JERRY_ID);
                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession(writesAndReads())) {
                Employee jerry = session.load(Employee.class, JERRY_ID);
                String jerryChangeVector = session.advanced().getChangeVectorFor(jerry);

                InMemoryDocumentSessionOperations inMemSession = inMem(session);
                Employee differentJerry = new Employee();
                differentJerry.setFirstName("Different Jerry");
                DocumentInfo documentInfo = buildExternalDocumentInfo(JERRY_ID, differentJerry, jerryChangeVector, "Employees");

                assertThatThrownBy(() -> inMemSession.registerExternalLoadedIntoTheSession(documentInfo))
                        .isInstanceOf(IllegalStateException.class)
                        .hasMessageContaining("is already in the session with a different entity instance");
            }
        }
    }

    @Test
    public void shouldAllowReregisteringSameEntityInstance() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                Employee jerry = new Employee();
                jerry.setFirstName("Jerry");
                session.store(jerry, JERRY_ID);
                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession(writesAndReads())) {
                Employee jerry = session.load(Employee.class, JERRY_ID);
                String jerryChangeVector = session.advanced().getChangeVectorFor(jerry);

                InMemoryDocumentSessionOperations inMemSession = inMem(session);
                DocumentInfo documentInfo = buildExternalDocumentInfo(JERRY_ID, jerry, jerryChangeVector, "Employees");

                // Should not throw - same instance is ok
                inMemSession.registerExternalLoadedIntoTheSession(documentInfo);

                Reference<String> trackedCv = new Reference<>();
                assertThat(inMemSession.getTrackedEntitiesHolder().tryGetValue(JERRY_ID, trackedCv)).isTrue();
                assertThat(trackedCv.value).isEqualTo(jerryChangeVector);
            }
        }
    }

    @Test
    public void shouldTrackMultipleExternalEntities_AndDetectConcurrencyViolations() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                Employee jerry = new Employee();
                jerry.setFirstName("Jerry");
                session.store(jerry, JERRY_ID);

                Employee egor = new Employee();
                egor.setFirstName("Egor");
                session.store(egor, EGOR_ID);

                Employee alice = new Employee();
                alice.setFirstName("Alice");
                session.store(alice, "employees/3-A");

                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession(writesAndReads())) {
                session.load(Employee.class, JERRY_ID);

                InMemoryDocumentSessionOperations inMemSession = inMem(session);

                Employee egor;
                String egorCv;
                try (IDocumentSession externalSession = store.openSession()) {
                    egor = externalSession.load(Employee.class, EGOR_ID);
                    egorCv = externalSession.advanced().getChangeVectorFor(egor);
                }
                inMemSession.registerExternalLoadedIntoTheSession(buildExternalDocumentInfo(EGOR_ID, egor, egorCv, "Employees"));

                Employee alice;
                String aliceCv;
                try (IDocumentSession externalSession = store.openSession()) {
                    alice = externalSession.load(Employee.class, "employees/3-A");
                    aliceCv = externalSession.advanced().getChangeVectorFor(alice);
                }
                inMemSession.registerExternalLoadedIntoTheSession(buildExternalDocumentInfo("employees/3-A", alice, aliceCv, "Employees"));

                Reference<String> ref = new Reference<>();
                assertThat(inMemSession.getTrackedEntitiesHolder().tryGetValue(JERRY_ID, ref)).isTrue();
                assertThat(inMemSession.getTrackedEntitiesHolder().tryGetValue(EGOR_ID, ref)).isTrue();
                assertThat(inMemSession.getTrackedEntitiesHolder().tryGetValue("employees/3-A", ref)).isTrue();

                String actualAliceCv;
                try (IDocumentSession s = store.openSession()) {
                    Employee a = s.load(Employee.class, "employees/3-A");
                    a.setFirstName("Alicia");
                    s.saveChanges();
                    actualAliceCv = s.advanced().getChangeVectorFor(a);
                }

                // external entities have Document=null, so a PUT is generated and the error comes from the PUT command.
                ConcurrencyException ex = assertConcurrency(session::saveChanges);

                assertThat(ex.getMessage())
                        .contains("Document employees/3-A has change vector " + actualAliceCv + ", but Put was called with change vector " + aliceCv);
                assertThat(ex.getId()).isEqualTo("employees/3-A");
                assertThat(ex.getActualChangeVector()).isEqualTo(actualAliceCv);
                assertThat(ex.getExpectedChangeVector()).isEqualTo(aliceCv);
            }
        }
    }

    @Test
    public void shouldNotTrackExternalEntity_WhenNoTrackingModeEnabled() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                Employee jerry = new Employee();
                jerry.setFirstName("Jerry");
                session.store(jerry, JERRY_ID);

                Employee egor = new Employee();
                egor.setFirstName("Egor");
                session.store(egor, EGOR_ID);

                session.saveChanges();
            }

            SessionOptions noTracking = new SessionOptions();
            noTracking.setNoTracking(true);

            try (IDocumentSession session = store.openSession(noTracking)) {
                InMemoryDocumentSessionOperations inMemSession = inMem(session);

                Employee egor;
                String egorCv;
                try (IDocumentSession externalSession = store.openSession()) {
                    egor = externalSession.load(Employee.class, EGOR_ID);
                    egorCv = externalSession.advanced().getChangeVectorFor(egor);
                }

                DocumentInfo documentInfo = buildExternalDocumentInfo(EGOR_ID, egor, egorCv, "Employees");

                // Should not track in NoTracking mode
                inMemSession.registerExternalLoadedIntoTheSession(documentInfo);

                Reference<String> ref = new Reference<>();
                assertThat(inMemSession.getTrackedEntitiesHolder().tryGetValue(EGOR_ID, ref)).isFalse();

                try (IDocumentSession s = store.openSession()) {
                    Employee e = s.load(Employee.class, EGOR_ID);
                    e.setFirstName("Greg");
                    s.saveChanges();
                }

                session.saveChanges(); // Should not throw
            }
        }
    }

    @Test
    public void shouldRemoveFromIncluded_WhenRegisteringExternalEntityThatWasIncluded() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            String addressId;
            try (IDocumentSession session = store.openSession()) {
                Address address = new Address();
                address.setCity("Harish");
                address.setStreet("Erets Rd");
                session.store(address);
                addressId = session.advanced().getDocumentId(address);
                address.setId(addressId);

                Employee jerry = new Employee();
                jerry.setFirstName("Jerry");
                jerry.setAddress(address);
                session.store(jerry, JERRY_ID);
                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession(writesAndReads())) {
                session.include("address.id").load(Employee.class, JERRY_ID);

                InMemoryDocumentSessionOperations inMemSession = inMem(session);

                assertThat(inMemSession.includedDocumentsById.containsKey(addressId)).isTrue();
                Reference<String> ref = new Reference<>();
                assertThat(inMemSession.getTrackedEntitiesHolder().tryGetValue(addressId, ref)).isTrue();

                Address externalAddress;
                String addressCv;
                try (IDocumentSession externalSession = store.openSession()) {
                    externalAddress = externalSession.load(Address.class, addressId);
                    addressCv = externalSession.advanced().getChangeVectorFor(externalAddress);
                }

                DocumentInfo documentInfo = buildExternalDocumentInfo(addressId, externalAddress, addressCv, "Addresses");
                inMemSession.registerExternalLoadedIntoTheSession(documentInfo);

                assertThat(inMemSession.includedDocumentsById.containsKey(addressId)).isFalse();
                Reference<String> trackedCv = new Reference<>();
                assertThat(inMemSession.getTrackedEntitiesHolder().tryGetValue(addressId, trackedCv)).isTrue();
                assertThat(trackedCv.value).isEqualTo(addressCv);

                String actualAddressCv;
                try (IDocumentSession s = store.openSession()) {
                    Address a = s.load(Address.class, addressId);
                    a.setCity("Hadera");
                    s.saveChanges();
                    actualAddressCv = s.advanced().getChangeVectorFor(a);
                }

                // external entities have Document=null, so a PUT is generated and the error comes from the PUT command.
                ConcurrencyException ex = assertConcurrency(session::saveChanges);

                assertThat(ex.getMessage())
                        .contains("Document " + addressId + " has change vector " + actualAddressCv + ", but Put was called with change vector " + addressCv);
                assertThat(ex.getId()).isEqualTo(addressId);
                assertThat(ex.getActualChangeVector()).isEqualTo(actualAddressCv);
                assertThat(ex.getExpectedChangeVector()).isEqualTo(addressCv);
            }
        }
    }

    @Test
    public void shouldThrowConcurrencyException_WhenTrackedEntityLoadedButThenWasDeletedInBackgroundSession() throws Exception {
        shouldThrowConcurrencyException_WhenTrackedEntityLoadedButThenWasDeletedInternal(false);
    }

    @Test
    public void shouldThrowConcurrencyException_WhenTrackedEntityLoadedButThenWasDeletedByIdInBackgroundSession() throws Exception {
        shouldThrowConcurrencyException_WhenTrackedEntityLoadedButThenWasDeletedInternal(true);
    }

    private void shouldThrowConcurrencyException_WhenTrackedEntityLoadedButThenWasDeletedInternal(boolean deleteById) throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            seedJerryAndEgor(store);

            try (IDocumentSession session = store.openSession(writesAndReads())) {
                Employee jerry = session.load(Employee.class, JERRY_ID);

                String expected = session.advanced().getChangeVectorFor(jerry);
                try (IDocumentSession s = store.openSession()) {
                    if (!deleteById) {
                        Employee j = s.load(Employee.class, JERRY_ID);
                        s.delete(j);
                    } else {
                        s.delete(JERRY_ID);
                    }
                    s.saveChanges();
                }

                ConcurrencyException e = assertConcurrency(session::saveChanges);

                assertThat(e.getMessage())
                        .contains("Document 'employees/1-A' has been modified since it was loaded. The expected change vector '"
                                + expected + "' does not match the current change vector 'string.Empty'.");
                assertThat(e.getId()).isEqualTo(JERRY_ID);
                assertThat(e.getActualChangeVector()).isNullOrEmpty();
                assertThat(e.getExpectedChangeVector()).isEqualTo(expected);
            }

            try (IDocumentSession session = store.openSession(writesAndReads())) {
                Employee egor = session.load(Employee.class, EGOR_ID);
                assertThat(egor.getFirstName()).isEqualTo("Egor");
                assertThat(egor.getAddress().getStreet()).isEqualTo("Ahad Ha'am");

                Employee j = session.load(Employee.class, JERRY_ID);
                assertThat(j).isNull();
            }
        }
    }

    @ParameterizedTest
    @CsvSource({"true,true", "true,false", "false,true", "false,false"})
    public void shouldThrowConcurrencyException_WhenTrackedEntityLoadedButThenWasDeletedInBackgroundSession2(boolean deleteById, boolean deleteByIdInBackgroundSession) throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            seedJerryAndEgor(store);

            try (IDocumentSession session = store.openSession(writesAndReads())) {
                String expected = null;
                if (!deleteByIdInBackgroundSession) {
                    Employee jerry = session.load(Employee.class, JERRY_ID);
                    expected = session.advanced().getChangeVectorFor(jerry);
                    session.delete(jerry);
                } else {
                    session.delete(JERRY_ID);
                }

                try (IDocumentSession s = store.openSession()) {
                    if (!deleteById) {
                        Employee j = s.load(Employee.class, JERRY_ID);
                        s.delete(j);
                    } else {
                        s.delete(JERRY_ID);
                    }
                    s.saveChanges();
                }

                if (!deleteByIdInBackgroundSession) {
                    // The DELETE command detects the doc was already deleted (tombstone) and throws with the expected CV
                    String expectedCv = expected;
                    ConcurrencyException e = assertConcurrency(session::saveChanges);
                    assertThat(e.getMessage()).contains("does not exist, but delete was called with change vector");
                    assertThat(e.getId()).isEqualTo("employees/1-a"); // tombstone stores lowercase ID
                    assertThat(e.getActualChangeVector()).isNull(); // doc no longer exists, so no actual CV
                    assertThat(e.getExpectedChangeVector()).isEqualTo(expectedCv);
                } else {
                    // this should not throw concurrency exception for jerry
                    session.saveChanges();
                }
            }

            try (IDocumentSession session = store.openSession(writesAndReads())) {
                Employee egor = session.load(Employee.class, EGOR_ID);
                assertThat(egor.getFirstName()).isEqualTo("Egor");
                assertThat(egor.getAddress().getStreet()).isEqualTo("Ahad Ha'am");

                Employee j = session.load(Employee.class, JERRY_ID);
                assertThat(j).isNull();
            }
        }
    }

    @ParameterizedTest
    @MethodSource("sessionActions")
    public void shouldThrowConcurrencyException_WhenOptimisticConcurrencyModeIsSetViaConventions(NamedAction sessionAction) throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            try (DocumentStore customStore = openStoreWithConventions(store, OptimisticConcurrencyMode.WRITES_AND_READS)) {
                seedJerryAndEgor(customStore);

                // open session without explicit SessionOptions - should inherit from conventions
                try (IDocumentSession session = customStore.openSession()) {
                    Employee jerry = session.load(Employee.class, JERRY_ID);

                    sessionAction.invoke(session);

                    String expected = session.advanced().getChangeVectorFor(jerry);
                    assertThat(expected).isNotEmpty();

                    modifyJerryInBackground(customStore);

                    ConcurrencyException e = assertConcurrency(session::saveChanges);

                    assertThat(e.getMessage()).contains("Document 'employees/1-A' has been modified");
                    assertThat(e.getId()).isEqualTo(JERRY_ID);
                }

                try (IDocumentSession session = customStore.openSession()) {
                    Employee egor = session.load(Employee.class, EGOR_ID);
                    assertThat(egor.getFirstName()).isEqualTo("Egor");
                    assertThat(egor.getAddress().getStreet()).isEqualTo("Ahad Ha'am");

                    Employee j = session.load(Employee.class, JERRY_ID);
                    assertThat(j.getAddress().getCity()).isEqualTo("Hadera");
                }
            }
        }
    }

    @Test
    public void sessionOptions_ShouldOverride_Conventions() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            try (DocumentStore customStore = openStoreWithConventions(store, OptimisticConcurrencyMode.WRITES_AND_READS)) {
                try (IDocumentSession session = customStore.openSession()) {
                    Employee jerry = new Employee();
                    jerry.setFirstName("Jerry");
                    session.store(jerry, JERRY_ID);
                    session.saveChanges();
                }

                SessionOptions noneOptions = new SessionOptions();
                noneOptions.setOptimisticConcurrencyMode(OptimisticConcurrencyMode.NONE);

                // session explicitly sets None - should override the convention
                try (IDocumentSession session = customStore.openSession(noneOptions)) {
                    session.load(Employee.class, JERRY_ID);

                    try (IDocumentSession s = customStore.openSession()) {
                        Employee j = s.load(Employee.class, JERRY_ID);
                        Address address = new Address();
                        address.setCity("Hadera");
                        j.setAddress(address);
                        s.saveChanges();
                    }

                    // should NOT throw because session overrides convention with None
                    session.saveChanges();
                }
            }
        }
    }

    @Test
    public void writesAndReads_ShouldStillDetectConcurrencyOnReadOnlyEntity_WhenModifiedEntityIsExcludedFromTrackCommand() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            seedJerryAndEgor(store);

            try (IDocumentSession session = store.openSession(writesAndReads())) {
                session.load(Employee.class, JERRY_ID);   // read-only (not modified)
                Employee egor = session.load(Employee.class, EGOR_ID);   // will be modified

                egor.setAddress(street("New Street"));

                try (IDocumentSession s = store.openSession()) {
                    Employee j = s.load(Employee.class, JERRY_ID);
                    Address address = new Address();
                    address.setCity("Hadera");
                    j.setAddress(address);
                    s.saveChanges();
                }

                ConcurrencyException e = assertConcurrency(session::saveChanges);
                assertThat(e.getMessage()).contains("employees/1-A");
            }
        }
    }

    @Test
    public void writesAndReads_ShouldStillDetectConcurrencyOnModifiedEntity_ViaPutCommand() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            seedJerryAndEgor(store);

            try (IDocumentSession session = store.openSession(writesAndReads())) {
                session.load(Employee.class, JERRY_ID);   // read-only
                Employee egor = session.load(Employee.class, EGOR_ID);   // will be modified

                egor.setAddress(street("New Street"));

                try (IDocumentSession s = store.openSession()) {
                    Employee e2 = s.load(Employee.class, EGOR_ID);
                    e2.setAddress(street("Background Street"));
                    s.saveChanges();
                }

                ConcurrencyException e = assertConcurrency(session::saveChanges);
                assertThat(e.getMessage()).contains("employees/2-A");
            }
        }
    }

    @Test
    public void writesAndReads_ModifiedEntityShouldBeExcludedFromBatchTrackChangesCommand() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                Employee jerry = new Employee();
                jerry.setFirstName("Jerry");
                session.store(jerry, JERRY_ID);

                Employee egor = new Employee();
                egor.setFirstName("Egor");
                session.store(egor, EGOR_ID);

                Employee dana = new Employee();
                dana.setFirstName("Dana");
                session.store(dana, "employees/3-A");

                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession(writesAndReads())) {
                InMemoryDocumentSessionOperations internalSession = inMem(session);

                session.load(Employee.class, JERRY_ID);   // read-only
                Employee egor = session.load(Employee.class, EGOR_ID);    // will be modified
                session.load(Employee.class, "employees/3-A");    // read-only

                egor.setFirstName("Egor Modified");

                InMemoryDocumentSessionOperations.SaveChangesData saveChangesData = internalSession.prepareForSaveChanges();

                // egor's ID should be in the skip set (because the PUT command already checks concurrency for it)
                assertThat(saveChangesData.getIdsAlreadyCheckedForConcurrency()).contains(EGOR_ID);

                // jerry and dana should NOT be in the skip set (read-only, only checked by BatchTrackChanges)
                assertThat(saveChangesData.getIdsAlreadyCheckedForConcurrency()).doesNotContain(JERRY_ID);
                assertThat(saveChangesData.getIdsAlreadyCheckedForConcurrency()).doesNotContain("employees/3-A");

                assertThat(saveChangesData.getTrackChangesCommandData()).isNotNull();

                BatchTrackChangesCommandData trackChangesCommandData = saveChangesData.getTrackChangesCommandData();
                assertThat(trackChangesCommandData.getTrackedEntities()).containsKey(JERRY_ID);
                assertThat(trackChangesCommandData.getTrackedEntities()).containsKey(EGOR_ID);
                assertThat(trackChangesCommandData.getTrackedEntities()).containsKey("employees/3-A");

                // the serialized command should NOT include the modified entity
                ObjectNode json = serializeCommand(trackChangesCommandData, store.getConventions());
                ObjectNode trackedEntities = (ObjectNode) json.get("TrackedEntities");
                assertThat(trackedEntities).isNotNull();

                assertThat(trackedEntities.has(JERRY_ID)).isTrue();
                assertThat(trackedEntities.has("employees/3-A")).isTrue();

                // egor (modified) should NOT be in the JSON sent to the server - no duplicate check
                assertThat(trackedEntities.has(EGOR_ID)).isFalse();
            }
        }
    }

    @Test
    public void writesAndReads_DeferredDeleteById_IsStillCheckedByBatchTrackChangesCommand() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                Employee jerry = new Employee();
                jerry.setFirstName("Jerry");
                session.store(jerry, JERRY_ID);

                Employee egor = new Employee();
                egor.setFirstName("Egor");
                session.store(egor, EGOR_ID);

                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession(writesAndReads())) {
                InMemoryDocumentSessionOperations internalSession = inMem(session);

                session.load(Employee.class, JERRY_ID);   // read-only
                session.load(Employee.class, EGOR_ID);    // will be deleted by ID

                // delete by string ID — goes through Defer path (not PrepareForEntitiesDeletion)
                session.delete(EGOR_ID);

                InMemoryDocumentSessionOperations.SaveChangesData saveChangesData = internalSession.prepareForSaveChanges();

                // egor's deferred delete is NOT in the skip set
                assertThat(saveChangesData.getIdsAlreadyCheckedForConcurrency()).doesNotContain(EGOR_ID);

                assertThat(saveChangesData.getTrackChangesCommandData()).isNotNull();
                ObjectNode json = serializeCommand(saveChangesData.getTrackChangesCommandData(), store.getConventions());
                ObjectNode trackedEntities = (ObjectNode) json.get("TrackedEntities");
                assertThat(trackedEntities).isNotNull();

                assertThat(trackedEntities.has(JERRY_ID)).isTrue();
                assertThat(trackedEntities.has(EGOR_ID)).isTrue(); // still checked by BatchTrackChanges
            }
        }
    }

    @Test
    public void writesFromConventions_ShouldBeInherited_AndNotTrackEntities() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            try (DocumentStore customStore = openStoreWithConventions(store, OptimisticConcurrencyMode.WRITES)) {
                try (IDocumentSession session = customStore.openSession()) {
                    Employee jerry = new Employee();
                    jerry.setFirstName("Jerry");
                    session.store(jerry, JERRY_ID);
                    session.saveChanges();
                }

                // open session without explicit OptimisticConcurrencyMode - should inherit Writes from conventions
                try (IDocumentSession session = customStore.openSession()) {
                    InMemoryDocumentSessionOperations internalSession = inMem(session);
                    assertThat(internalSession.getOptimisticConcurrencyMode()).isEqualTo(OptimisticConcurrencyMode.WRITES);

                    Employee jerry = session.load(Employee.class, JERRY_ID);

                    // Writes mode should NOT track entities (only WritesAndReads does)
                    Reference<String> ref = new Reference<>();
                    assertThat(internalSession.getTrackedEntitiesHolder().tryGetValue(JERRY_ID, ref)).isFalse();

                    jerry.setFirstName("Modified");

                    try (IDocumentSession s = customStore.openSession()) {
                        Employee j = s.load(Employee.class, JERRY_ID);
                        j.setFirstName("Background");
                        s.saveChanges();
                    }

                    // should throw because the PUT has a change vector (Writes mode)
                    ConcurrencyException e = assertConcurrency(session::saveChanges);
                    assertThat(e.getMessage()).contains("employees/1-A");
                }
            }
        }
    }

    @Test
    public void writesAndReads_ConcurrencyCheckModeDisabled_ShouldStillBeTracked() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                Employee jerry = new Employee();
                jerry.setFirstName("Jerry");
                session.store(jerry, JERRY_ID);
                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession(writesAndReads())) {
                session.load(Employee.class, JERRY_ID);

                // Store with null changeVector → ConcurrencyCheckMode.Disabled
                Employee newEmployee = new Employee();
                newEmployee.setFirstName("NewEmployee");
                session.store(newEmployee, null, EGOR_ID);

                try (IDocumentSession s = store.openSession()) {
                    Employee j = s.load(Employee.class, JERRY_ID);
                    j.setFirstName("Background");
                    s.saveChanges();
                }

                // should throw for jerry via BatchTrackChangesCommand
                ConcurrencyException e = assertConcurrency(session::saveChanges);
                assertThat(e.getMessage()).contains("employees/1-A");
            }
        }
    }
}