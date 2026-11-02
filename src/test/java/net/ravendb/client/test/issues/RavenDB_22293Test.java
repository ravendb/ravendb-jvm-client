package net.ravendb.client.test.issues;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.DocumentStore;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.IdTypeAndName;
import net.ravendb.client.documents.commands.batches.CommandType;
import net.ravendb.client.documents.commands.batches.ICommandData;
import net.ravendb.client.documents.conventions.SessionPatchBehavior;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.documents.session.InMemoryDocumentSessionOperations;
import net.ravendb.client.documents.session.OptimisticConcurrencyMode;
import net.ravendb.client.exceptions.RavenException;
import net.ravendb.client.infrastructure.EnableOnServer;
import net.ravendb.client.infrastructure.entities.Address;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.io.IOException;
import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@EnableOnServer(thresholdVersion = "7.2")
public class RavenDB_22293Test extends RemoteTestBase {

    @ParameterizedTest
    @EnumSource(SessionPatchBehavior.class)
    public void patch_simpleProperty_usesConfiguredBehavior(SessionPatchBehavior behavior) throws Exception {
        try (IDocumentStore baseStore = getDocumentStore();
             IDocumentStore store = openStore(baseStore, behavior)) {
            storeUser(store, user("Original", 25));

            try (IDocumentSession session = store.openSession()) {
                session.advanced().patch("users/1", "name", "Updated");
                assertUsesPatchBehavior(session, "users/1", behavior);
                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                UserWithTags user = session.load(UserWithTags.class, "users/1");
                assertThat(user.getName()).isEqualTo("Updated");
                assertThat(user.getAge()).isEqualTo(25);
            }
        }
    }

    @ParameterizedTest
    @EnumSource(SessionPatchBehavior.class)
    public void patch_arrayAdd_usesConfiguredBehavior(SessionPatchBehavior behavior) throws Exception {
        try (IDocumentStore baseStore = getDocumentStore();
             IDocumentStore store = openStore(baseStore, behavior)) {
            storeUser(store, userWithTags("a", "b"));

            try (IDocumentSession session = store.openSession()) {
                session.advanced().<UserWithTags, String>patchArray("users/1", "tags", tags -> tags.add("c"));
                assertUsesPatchBehavior(session, "users/1", behavior);
                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                assertThat(session.load(UserWithTags.class, "users/1").getTags()).containsExactly("a", "b", "c");
            }
        }
    }

    @ParameterizedTest
    @EnumSource(SessionPatchBehavior.class)
    public void patch_arrayRemoveAt_usesConfiguredBehavior(SessionPatchBehavior behavior) throws Exception {
        try (IDocumentStore baseStore = getDocumentStore();
             IDocumentStore store = openStore(baseStore, behavior)) {
            storeUser(store, userWithTags("a", "b", "c"));

            try (IDocumentSession session = store.openSession()) {
                session.advanced().<UserWithTags, String>patchArray("users/1", "tags", tags -> tags.removeAt(1));
                assertUsesPatchBehavior(session, "users/1", behavior);
                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                assertThat(session.load(UserWithTags.class, "users/1").getTags()).containsExactly("a", "c");
            }
        }
    }

    @ParameterizedTest
    @EnumSource(SessionPatchBehavior.class)
    public void patch_mapPut_usesConfiguredBehavior(SessionPatchBehavior behavior) throws Exception {
        try (IDocumentStore baseStore = getDocumentStore();
             IDocumentStore store = openStore(baseStore, behavior)) {
            storeUser(store, userWithSettings("theme", "light"));

            try (IDocumentSession session = store.openSession()) {
                session.advanced().<UserWithTags, String, String>patchObject("users/1", "settings", settings -> settings.put("lang", "en"));
                assertUsesPatchBehavior(session, "users/1", behavior);
                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                Map<String, String> settings = session.load(UserWithTags.class, "users/1").getSettings();
                assertThat(settings).hasSize(2)
                        .containsEntry("theme", "light")
                        .containsEntry("lang", "en");
            }
        }
    }

    @ParameterizedTest
    @EnumSource(SessionPatchBehavior.class)
    public void patch_mapRemove_usesConfiguredBehavior(SessionPatchBehavior behavior) throws Exception {
        try (IDocumentStore baseStore = getDocumentStore();
             IDocumentStore store = openStore(baseStore, behavior)) {
            storeUser(store, userWithSettings("theme", "light", "lang", "en"));

            try (IDocumentSession session = store.openSession()) {
                session.advanced().<UserWithTags, String, String>patchObject("users/1", "settings", settings -> settings.remove("lang"));
                assertUsesPatchBehavior(session, "users/1", behavior);
                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                assertThat(session.load(UserWithTags.class, "users/1").getSettings())
                        .hasSize(1)
                        .containsEntry("theme", "light");
            }
        }
    }

    @Test
    public void patch_nestedProperty_usesJsonPatch() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            Address address = new Address();
            address.setCity("OldCity");
            address.setCountry("US");
            UserWithTags user = user("Test", 0);
            user.setAddress(address);
            storeUser(store, user);

            try (IDocumentSession session = store.openSession()) {
                session.advanced().patch("users/1", "address.city", "NewCity");
                assertThat(hasDeferredCommand(session, "users/1", CommandType.JSON_PATCH)).isTrue();
                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                Address loaded = session.load(UserWithTags.class, "users/1").getAddress();
                assertThat(loaded.getCity()).isEqualTo("NewCity");
                assertThat(loaded.getCountry()).isEqualTo("US");
            }
        }
    }

    @Test
    public void patch_nullValue_usesJsonPatch() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            storeUser(store, user("Test", 0));

            try (IDocumentSession session = store.openSession()) {
                session.advanced().patch("users/1", "name", null);
                assertThat(hasDeferredCommand(session, "users/1", CommandType.JSON_PATCH)).isTrue();
                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                assertThat(session.load(UserWithTags.class, "users/1").getName()).isNull();
            }
        }
    }

    @Test
    public void patch_integerValue_usesJsonPatch() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            storeUser(store, user("Test", 25));

            try (IDocumentSession session = store.openSession()) {
                session.advanced().patch("users/1", "age", 30);
                assertThat(hasDeferredCommand(session, "users/1", CommandType.JSON_PATCH)).isTrue();
                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                assertThat(session.load(UserWithTags.class, "users/1").getAge()).isEqualTo(30);
            }
        }
    }

    @Test
    public void patch_multipleProperties_mergesJsonPatch() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            storeUser(store, user("Original", 25));

            try (IDocumentSession session = store.openSession()) {
                session.advanced().patch("users/1", "name", "Updated");
                session.advanced().patch("users/1", "age", 30);

                assertThat(hasDeferredCommand(session, "users/1", CommandType.JSON_PATCH)).isTrue();
                assertThat(((InMemoryDocumentSessionOperations) session).getDeferredCommandsCount()).isEqualTo(1);

                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                UserWithTags user = session.load(UserWithTags.class, "users/1");
                assertThat(user.getName()).isEqualTo("Updated");
                assertThat(user.getAge()).isEqualTo(30);
            }
        }
    }

    @Test
    public void patch_trackedEntity_updatesAfterSaveChanges() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            storeUser(store, user("Original", 0));

            try (IDocumentSession session = store.openSession()) {
                UserWithTags user = session.load(UserWithTags.class, "users/1");
                session.advanced().patch(user, "name", "Updated");

                assertThat(hasDeferredCommand(session, "users/1", CommandType.JSON_PATCH)).isTrue();

                String changeVector = session.advanced().getChangeVectorFor(user);

                session.saveChanges();

                assertThat(user.getName()).isEqualTo("Updated");
                assertThat(session.advanced().getChangeVectorFor(user)).isNotEqualTo(changeVector);

                user.setAge(30);
                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                UserWithTags user = session.load(UserWithTags.class, "users/1");
                assertThat(user.getName()).isEqualTo("Updated");
                assertThat(user.getAge()).isEqualTo(30);
            }
        }
    }

    @Test
    public void patch_arrayAddMultiple_usesJsonPatch() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            storeUser(store, userWithTags("a"));

            try (IDocumentSession session = store.openSession()) {
                session.advanced().<UserWithTags, String>patchArray("users/1", "tags", tags -> tags.add("b", "c"));
                assertThat(hasDeferredCommand(session, "users/1", CommandType.JSON_PATCH)).isTrue();
                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                assertThat(session.load(UserWithTags.class, "users/1").getTags()).containsExactly("a", "b", "c");
            }
        }
    }

    @Test
    public void patch_complexValue_fallsBackToJavaScript() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            storeUser(store, user("Test", 0));

            Address address = new Address();
            address.setCity("Hadera");

            try (IDocumentSession session = store.openSession()) {
                session.advanced().patch("users/1", "address", address);

                assertThat(hasDeferredCommand(session, "users/1", CommandType.PATCH)).isTrue();
                assertThat(hasDeferredCommand(session, "users/1", CommandType.JSON_PATCH)).isFalse();

                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                assertThat(session.load(UserWithTags.class, "users/1").getAddress().getCity()).isEqualTo("Hadera");
            }
        }
    }

    @Test
    public void patch_localDateAndLocalTime_fallBackToJavaScript() throws Exception {
        // LocalDate/LocalTime mirror C# DateOnly/TimeOnly, which stay on the JavaScript patch path
        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                session.advanced().patch("users/1", "birthday", LocalDate.of(2000, 1, 1));

                assertThat(hasDeferredCommand(session, "users/1", CommandType.PATCH)).isTrue();
                assertThat(hasDeferredCommand(session, "users/1", CommandType.JSON_PATCH)).isFalse();
            }

            try (IDocumentSession session = store.openSession()) {
                session.advanced().patch("users/1", "wakeUp", LocalTime.of(7, 30));

                assertThat(hasDeferredCommand(session, "users/1", CommandType.PATCH)).isTrue();
                assertThat(hasDeferredCommand(session, "users/1", CommandType.JSON_PATCH)).isFalse();
            }
        }
    }

    @Test
    public void patch_afterIncrement_fallsBackToJavaScript() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            storeUser(store, user("Original", 25));

            try (IDocumentSession session = store.openSession()) {
                // increment creates a JavaScript patch, so the following patch merges into it
                session.advanced().increment("users/1", "age", 5);
                session.advanced().patch("users/1", "name", "Updated");

                assertThat(hasDeferredCommand(session, "users/1", CommandType.PATCH)).isTrue();
                assertThat(hasDeferredCommand(session, "users/1", CommandType.JSON_PATCH)).isFalse();

                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                UserWithTags user = session.load(UserWithTags.class, "users/1");
                assertThat(user.getName()).isEqualTo("Updated");
                assertThat(user.getAge()).isEqualTo(30);
            }
        }
    }

    @Test
    public void patch_thenIncrement_keepsBothCommands() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            storeUser(store, user("Original", 25));

            try (IDocumentSession session = store.openSession()) {
                session.advanced().patch("users/1", "name", "Updated");
                session.advanced().increment("users/1", "age", 5);

                assertThat(hasDeferredCommand(session, "users/1", CommandType.JSON_PATCH)).isTrue();
                assertThat(hasDeferredCommand(session, "users/1", CommandType.PATCH)).isTrue();
                assertThat(((InMemoryDocumentSessionOperations) session).getDeferredCommandsCount()).isEqualTo(2);

                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                UserWithTags user = session.load(UserWithTags.class, "users/1");
                assertThat(user.getName()).isEqualTo("Updated");
                assertThat(user.getAge()).isEqualTo(30);
            }
        }
    }

    @Test
    public void patch_withOptimisticConcurrency_doesNotSendChangeVector_jsonPatch() throws Exception {
        // session patch does not enforce optimistic concurrency, so a concurrent modification
        // between load and patch + saveChanges must not throw ConcurrencyException
        try (IDocumentStore store = getDocumentStore()) {
            storeUser(store, user("Original", 25));

            try (IDocumentSession session = store.openSession()) {
                session.advanced().setOptimisticConcurrencyMode(OptimisticConcurrencyMode.WRITES);

                UserWithTags user = session.load(UserWithTags.class, "users/1");

                try (IDocumentSession otherSession = store.openSession()) {
                    otherSession.load(UserWithTags.class, "users/1").setAge(99);
                    otherSession.saveChanges();
                }

                session.advanced().patch(user, "name", "Updated");
                assertThat(hasDeferredCommand(session, "users/1", CommandType.JSON_PATCH)).isTrue();

                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                UserWithTags user = session.load(UserWithTags.class, "users/1");
                assertThat(user.getName()).isEqualTo("Updated");
                assertThat(user.getAge()).isEqualTo(99);
            }
        }
    }

    @Test
    public void patch_withOptimisticConcurrency_doesNotSendChangeVector_javaScript() throws Exception {
        // same as above but through the JavaScript path (increment always uses JavaScript),
        // to confirm both paths behave identically with optimistic concurrency
        try (IDocumentStore store = getDocumentStore()) {
            storeUser(store, user("Original", 25));

            try (IDocumentSession session = store.openSession()) {
                session.advanced().setOptimisticConcurrencyMode(OptimisticConcurrencyMode.WRITES);

                UserWithTags user = session.load(UserWithTags.class, "users/1");

                try (IDocumentSession otherSession = store.openSession()) {
                    otherSession.load(UserWithTags.class, "users/1").setAge(99);
                    otherSession.saveChanges();
                }

                session.advanced().increment(user, "age", 1);
                assertThat(hasDeferredCommand(session, "users/1", CommandType.PATCH)).isTrue();

                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                assertThat(session.load(UserWithTags.class, "users/1").getAge()).isEqualTo(100);
            }
        }
    }

    @Test
    public void increment_staysAsJavaScript() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            storeUser(store, user("Test", 10));

            try (IDocumentSession session = store.openSession()) {
                session.advanced().increment("users/1", "age", 5);

                assertThat(hasDeferredCommand(session, "users/1", CommandType.PATCH)).isTrue();
                assertThat(hasDeferredCommand(session, "users/1", CommandType.JSON_PATCH)).isFalse();

                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                assertThat(session.load(UserWithTags.class, "users/1").getAge()).isEqualTo(15);
            }
        }
    }

    @Test
    public void patch_absentProperty_createsPropertyViaJsonPatch() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            // a document without the 'age' property at all - a JsonPatch "replace" would throw on the server,
            // the JavaScript "this.age = value" silently created it
            try (IDocumentSession session = store.openSession()) {
                NameOnly nameOnly = new NameOnly();
                nameOnly.setName("Test");
                session.store(nameOnly, "users/1");
                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                session.advanced().patch("users/1", "age", 30);
                assertThat(hasDeferredCommand(session, "users/1", CommandType.JSON_PATCH)).isTrue();
                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                UserWithTags user = session.load(UserWithTags.class, "users/1");
                assertThat(user.getName()).isEqualTo("Test");
                assertThat(user.getAge()).isEqualTo(30);
            }
        }
    }

    @Test
    public void patch_arrayElementByIndex_replacesInPlace() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            storeUser(store, userWithTags("a", "b", "c"));

            try (IDocumentSession session = store.openSession()) {
                session.advanced().patch("users/1", "tags[1]", "B");
                assertThat(hasDeferredCommand(session, "users/1", CommandType.JSON_PATCH)).isTrue();
                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                // replaced in place, not inserted
                assertThat(session.load(UserWithTags.class, "users/1").getTags()).containsExactly("a", "B", "c");
            }
        }
    }

    @Test
    public void patch_value_usesStoreSerializationConventions() throws Exception {
        try (IDocumentStore baseStore = getDocumentStore()) {
            DocumentStore store = new DocumentStore(baseStore.getUrls(), baseStore.getDatabase());
            SimpleModule module = new SimpleModule();
            module.addSerializer(Temperature.class, new TemperatureSerializer());
            store.getConventions().getEntityMapper().registerModule(module);
            store.initialize();

            try {
                try (IDocumentSession session = store.openSession()) {
                    WeatherDoc weather = new WeatherDoc();
                    weather.setTemp(Temperature.COLD);
                    session.store(weather, "weather/1");
                    session.saveChanges();
                }

                try (IDocumentSession session = store.openSession()) {
                    session.advanced().patch("weather/1", "temp", Temperature.HOT);
                    assertThat(hasDeferredCommand(session, "weather/1", CommandType.JSON_PATCH)).isTrue();
                    session.saveChanges();
                }

                try (IDocumentSession session = store.openSession()) {
                    // serialized with the store's custom serializer, not the default enum name
                    ObjectNode weather = session.load(ObjectNode.class, "weather/1");
                    assertThat(weather.get("temp").asText()).isEqualTo("temp-HOT");
                }
            } finally {
                store.close();
            }
        }
    }

    @Test
    public void sessionPatchBehavior_defaultsToJsonPatch() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            assertThat(store.getConventions().getSessionPatchBehavior()).isEqualTo(SessionPatchBehavior.JSON_PATCH);
        }
    }

    @Test
    public void sessionPatchBehavior_arrayRemoveAtOutOfRange_throwsUnderJsonPatch_noOpUnderJavaScript() throws Exception {
        try (IDocumentStore baseStore = getDocumentStore()) {
            // JsonPatch: strict RFC 6902 "remove /tags/10" is rejected by the server
            try (IDocumentStore store = openStore(baseStore, SessionPatchBehavior.JSON_PATCH)) {
                storeUser(store, userWithTags("a", "b", "c"));

                try (IDocumentSession session = store.openSession()) {
                    session.advanced().<UserWithTags, String>patchArray("users/1", "tags", tags -> tags.removeAt(10));
                    assertThatThrownBy(session::saveChanges).isInstanceOf(RavenException.class);
                }
            }

            // JavaScript: splice(10, 1) is a silent no-op
            try (IDocumentStore store = openStore(baseStore, SessionPatchBehavior.JAVA_SCRIPT)) {
                try (IDocumentSession session = store.openSession()) {
                    session.advanced().<UserWithTags, String>patchArray("users/1", "tags", tags -> tags.removeAt(10));
                    session.saveChanges();
                }

                try (IDocumentSession session = store.openSession()) {
                    assertThat(session.load(UserWithTags.class, "users/1").getTags()).containsExactly("a", "b", "c");
                }
            }
        }
    }

    @Test
    public void sessionPatchBehavior_mapRemoveAbsentKey_throwsUnderJsonPatch_noOpUnderJavaScript() throws Exception {
        try (IDocumentStore baseStore = getDocumentStore()) {
            // JsonPatch: strict RFC 6902 "remove /settings/missing" is rejected by the server
            try (IDocumentStore store = openStore(baseStore, SessionPatchBehavior.JSON_PATCH)) {
                storeUser(store, userWithSettings("theme", "light"));

                try (IDocumentSession session = store.openSession()) {
                    session.advanced().<UserWithTags, String, String>patchObject("users/1", "settings", settings -> settings.remove("missing"));
                    assertThatThrownBy(session::saveChanges).isInstanceOf(RavenException.class);
                }
            }

            // JavaScript: delete obj["missing"] is a silent no-op
            try (IDocumentStore store = openStore(baseStore, SessionPatchBehavior.JAVA_SCRIPT)) {
                try (IDocumentSession session = store.openSession()) {
                    session.advanced().<UserWithTags, String, String>patchObject("users/1", "settings", settings -> settings.remove("missing"));
                    session.saveChanges();
                }

                try (IDocumentSession session = store.openSession()) {
                    assertThat(session.load(UserWithTags.class, "users/1").getSettings())
                            .hasSize(1)
                            .containsEntry("theme", "light");
                }
            }
        }
    }

    private static IDocumentStore openStore(IDocumentStore baseStore, SessionPatchBehavior behavior) {
        DocumentStore store = new DocumentStore(baseStore.getUrls(), baseStore.getDatabase());
        store.getConventions().setSessionPatchBehavior(behavior);
        store.initialize();
        return store;
    }

    private static void assertUsesPatchBehavior(IDocumentSession session, String id, SessionPatchBehavior behavior) throws Exception {
        boolean jsonPatch = behavior == SessionPatchBehavior.JSON_PATCH;
        assertThat(hasDeferredCommand(session, id, CommandType.JSON_PATCH)).isEqualTo(jsonPatch);
        assertThat(hasDeferredCommand(session, id, CommandType.PATCH)).isEqualTo(!jsonPatch);
    }

    @SuppressWarnings("unchecked")
    private static boolean hasDeferredCommand(IDocumentSession session, String id, CommandType type) throws Exception {
        Field field = InMemoryDocumentSessionOperations.class.getDeclaredField("deferredCommandsMap");
        field.setAccessible(true);
        Map<IdTypeAndName, ICommandData> deferredCommands = (Map<IdTypeAndName, ICommandData>) field.get(session);
        return deferredCommands.containsKey(IdTypeAndName.create(id, type, null));
    }

    private static void storeUser(IDocumentStore store, UserWithTags user) {
        try (IDocumentSession session = store.openSession()) {
            session.store(user, "users/1");
            session.saveChanges();
        }
    }

    private static UserWithTags user(String name, int age) {
        UserWithTags user = new UserWithTags();
        user.setName(name);
        user.setAge(age);
        return user;
    }

    private static UserWithTags userWithTags(String... tags) {
        UserWithTags user = user("Test", 0);
        user.setTags(new ArrayList<>(Arrays.asList(tags)));
        return user;
    }

    private static UserWithTags userWithSettings(String... keysAndValues) {
        Map<String, String> settings = new HashMap<>();
        for (int i = 0; i < keysAndValues.length; i += 2) {
            settings.put(keysAndValues[i], keysAndValues[i + 1]);
        }

        UserWithTags user = user("Test", 0);
        user.setSettings(settings);
        return user;
    }

    public static class UserWithTags {
        private String id;
        private String name;
        private int age;
        private List<String> tags;
        private Map<String, String> settings;
        private Address address;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getAge() {
            return age;
        }

        public void setAge(int age) {
            this.age = age;
        }

        public List<String> getTags() {
            return tags;
        }

        public void setTags(List<String> tags) {
            this.tags = tags;
        }

        public Map<String, String> getSettings() {
            return settings;
        }

        public void setSettings(Map<String, String> settings) {
            this.settings = settings;
        }

        public Address getAddress() {
            return address;
        }

        public void setAddress(Address address) {
            this.address = address;
        }
    }

    public static class NameOnly {
        private String name;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    public enum Temperature {
        COLD,
        HOT
    }

    public static class TemperatureSerializer extends StdSerializer<Temperature> {
        public TemperatureSerializer() {
            super(Temperature.class);
        }

        @Override
        public void serialize(Temperature value, JsonGenerator gen, SerializerProvider provider) throws IOException {
            gen.writeString("temp-" + value.name());
        }
    }

    public static class WeatherDoc {
        private String id;
        private Temperature temp;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public Temperature getTemp() {
            return temp;
        }

        public void setTemp(Temperature temp) {
            this.temp = temp;
        }
    }
}
