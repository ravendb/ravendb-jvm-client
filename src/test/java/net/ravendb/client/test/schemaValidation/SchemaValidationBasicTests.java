package net.ravendb.client.test.schemaValidation;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.operations.schemaValidation.ConfigureSchemaValidationOperation;
import net.ravendb.client.documents.operations.schemaValidation.GetSchemaValidationConfiguration;
import net.ravendb.client.documents.operations.schemaValidation.SchemaDefinition;
import net.ravendb.client.documents.operations.schemaValidation.SchemaValidationConfiguration;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.documents.session.SessionOptions;
import net.ravendb.client.documents.session.TransactionMode;
import net.ravendb.client.exceptions.schemavalidation.SchemaValidationException;
import net.ravendb.client.infrastructure.DisabledOnPullRequest;
import net.ravendb.client.infrastructure.EnableOnServer;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

@DisabledOnPullRequest
@EnableOnServer(thresholdVersion = "7.2")
public class SchemaValidationBasicTests extends RemoteTestBase {

    @Test
    public void store_shouldValidateSchema() throws Exception {
        String[] collections = new String[] { "Users", "users" };

        for (String collection : collections) {

            String schemaData =
                    "{\n" +
                            "  \"type\": \"object\",\n" +
                            "  \"properties\": {\n" +
                            "    \"age\": {\n" +
                            "      \"type\": \"integer\",\n" +
                            "      \"minimum\": 21,\n" +
                            "      \"maximum\": 67\n" +
                            "    }\n" +
                            "  }\n" +
                            "}";

            try (IDocumentStore store = getDocumentStore()) {

                SchemaValidationConfiguration cfg = new SchemaValidationConfiguration();

                Map<String, SchemaDefinition> validators = new LinkedHashMap<>();
                SchemaDefinition def = new SchemaDefinition();
                def.setSchema(schemaData);
                validators.put(collection, def);

                cfg.setValidatorsPerCollection(validators);

                store.maintenance().send(new ConfigureSchemaValidationOperation(cfg));

                try (IDocumentSession session = store.openSession()) {
                    User u = new User();
                    u.setAge(17);
                    session.store(u, "users/1");

                    Throwable error = catchThrowable(session::saveChanges);
                    assertThat(error)
                            .isInstanceOf(SchemaValidationException.class)
                            .hasMessageContaining("The value '17' at 'age' should be greater than or equal to 21");
                }

                try (IDocumentSession session = store.openSession()) {
                    User u = new User();
                    u.setAge(80);

                    session.store(u, "users/1");

                    Throwable error = catchThrowable(session::saveChanges);
                    assertThat(error)
                            .isInstanceOf(SchemaValidationException.class)
                            .hasMessageContaining("The value '80' at 'age' should be less than or equal to 67");
                }

                try (IDocumentSession session = store.openSession()) {
                    User u = new User();
                    u.setAge(39);

                    session.store(u, "users/1");
                    session.saveChanges();
                }
            }
        }
    }

    @Test
    public void storeClusterTransaction_shouldValidateSchema() throws Exception {
        String schemaData =
                "{\n" +
                        "  \"type\": \"object\",\n" +
                        "  \"properties\": {\n" +
                        "    \"age\": {\n" +
                        "      \"type\": \"integer\",\n" +
                        "      \"minimum\": 21,\n" +
                        "      \"maximum\": 67\n" +
                        "    }\n" +
                        "  }\n" +
                        "}";

        try (IDocumentStore store = getDocumentStore()) {

            SchemaDefinition def = new SchemaDefinition();
            def.setSchema(schemaData);

            Map<String, SchemaDefinition> validators = new LinkedHashMap<>();
            validators.put("Users", def);

            SchemaValidationConfiguration cfg = new SchemaValidationConfiguration();
            cfg.setValidatorsPerCollection(validators);

            store.maintenance().send(new ConfigureSchemaValidationOperation(cfg));

            SessionOptions options = new SessionOptions();
            options.setTransactionMode(TransactionMode.CLUSTER_WIDE);
            try (IDocumentSession session = store.openSession(options)) {

                User u = new User();
                u.setAge(17);

                session.store(u, "users/1");

                Throwable error = catchThrowable(session::saveChanges);
                assertThat(error)
                        .isInstanceOf(SchemaValidationException.class)
                        .hasMessageContaining("The value '17' at 'age' should be greater than or equal to 21");
            }

            try (IDocumentSession session = store.openSession(options)) {

                User u = new User();
                u.setAge(80);

                session.store(u, "users/1");

                Throwable error = catchThrowable(session::saveChanges);
                assertThat(error)
                        .isInstanceOf(SchemaValidationException.class)
                        .hasMessageContaining("The value '80' at 'age' should be less than or equal to 67");
            }

            try (IDocumentSession session = store.openSession(options)) {

                User u = new User();
                u.setAge(39);

                session.store(u, "users/1");
                session.saveChanges();
            }

            SchemaValidationConfiguration configuration =
                    store.maintenance().send(new GetSchemaValidationConfiguration());

            SchemaDefinition companiesDef = new SchemaDefinition();
            companiesDef.setSchema(schemaData);
            configuration.getValidatorsPerCollection().put("Companies", companiesDef);

            SchemaDefinition usersDef = configuration.getValidatorsPerCollection().get("Users");
            usersDef.setDisabled(true);

            store.maintenance().send(new ConfigureSchemaValidationOperation(configuration));

            try (IDocumentSession session = store.openSession(options)) {

                User u = new User();
                u.setAge(20);

                session.store(u, "users/2");
                session.saveChanges();
            }
        }
    }

    @Test
    public void disableSchemaAfterCreation_shouldWork() throws Exception {
        String schemaData =
                "{\n" +
                        "  \"type\": \"object\",\n" +
                        "  \"properties\": {\n" +
                        "    \"age\": {\n" +
                        "      \"type\": \"integer\",\n" +
                        "      \"minimum\": 21,\n" +
                        "      \"maximum\": 67\n" +
                        "    }\n" +
                        "  }\n" +
                        "}";

        try (IDocumentStore store = getDocumentStore()) {
            SchemaDefinition usersDef = new SchemaDefinition();
            usersDef.setSchema(schemaData);

            SchemaDefinition ordersDef = new SchemaDefinition();
            ordersDef.setSchema(schemaData);

            Map<String, SchemaDefinition> validators = new LinkedHashMap<>();
            validators.put("Users", usersDef);
            validators.put("Orders", ordersDef);

            SchemaValidationConfiguration cfg = new SchemaValidationConfiguration();
            cfg.setValidatorsPerCollection(validators);

            store.maintenance().send(new ConfigureSchemaValidationOperation(cfg));

            try (IDocumentSession session = store.openSession()) {
                User u = new User();
                u.setAge(17);

                session.store(u, "users/1");

                Throwable error = catchThrowable(session::saveChanges);
                assertThat(error)
                        .isInstanceOf(SchemaValidationException.class)
                        .hasMessageContaining("The value '17' at 'age' should be greater than or equal to 21");
            }

            cfg.getValidatorsPerCollection().get("Users").setDisabled(true);

            store.maintenance().send(new ConfigureSchemaValidationOperation(cfg));

            try (IDocumentSession session = store.openSession()) {
                User u = new User();
                u.setAge(80);

                session.store(u, "users/1");
                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                User u = new User();
                u.setAge(39);

                session.store(u, "users/1");
                session.saveChanges();
            }
        }
    }

    @Test
    public void canStartWithDisabledSchema_shouldWork() throws Exception {
        String schemaData =
                "{\n" +
                        "  \"type\": \"object\",\n" +
                        "  \"properties\": {\n" +
                        "    \"age\": {\n" +
                        "      \"type\": \"integer\",\n" +
                        "      \"minimum\": 21,\n" +
                        "      \"maximum\": 67\n" +
                        "    }\n" +
                        "  }\n" +
                        "}";

        try (IDocumentStore store = getDocumentStore()) {

            SchemaDefinition usersDef = new SchemaDefinition();
            usersDef.setSchema(schemaData);
            usersDef.setDisabled(true);

            SchemaDefinition ordersDef = new SchemaDefinition();
            ordersDef.setSchema(schemaData);

            Map<String, SchemaDefinition> validators = new LinkedHashMap<>();
            validators.put("Users", usersDef);
            validators.put("Orders", ordersDef);

            SchemaValidationConfiguration cfg = new SchemaValidationConfiguration();
            cfg.setValidatorsPerCollection(validators);

            store.maintenance().send(new ConfigureSchemaValidationOperation(cfg));

            try (IDocumentSession session = store.openSession()) {
                User u = new User();
                u.setAge(17);

                session.store(u, "users/1");
                session.saveChanges(); // no exception expected
            }

            cfg.getValidatorsPerCollection().get("Users").setDisabled(false);

            store.maintenance().send(new ConfigureSchemaValidationOperation(cfg));

            try (IDocumentSession session = store.openSession()) {
                User u = new User();
                u.setAge(80);

                session.store(u, "users/1");

                Throwable error = catchThrowable(session::saveChanges);
                assertThat(error)
                        .isInstanceOf(SchemaValidationException.class)
                        .hasMessageContaining("The value '80' at 'age' should be less than or equal to 67");
            }
        }
    }

    private static class User {
        private int age;

        public int getAge() {
            return age;
        }

        public void setAge(int age) {
            this.age = age;
        }
    }
}

