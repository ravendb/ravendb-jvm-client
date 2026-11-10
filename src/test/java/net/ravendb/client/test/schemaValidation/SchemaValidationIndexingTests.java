package net.ravendb.client.test.schemaValidation;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.node.ObjectNode;
import net.ravendb.client.Constants;
import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.BulkInsertOperation;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.commands.PutDocumentCommand;
import net.ravendb.client.documents.indexes.*;
import net.ravendb.client.documents.operations.indexes.PutIndexesOperation;
import net.ravendb.client.documents.operations.indexes.ResetIndexOperation;
import net.ravendb.client.documents.operations.schemaValidation.ConfigureSchemaValidationOperation;
import net.ravendb.client.documents.operations.schemaValidation.SchemaDefinition;
import net.ravendb.client.documents.operations.schemaValidation.SchemaValidationConfiguration;
import net.ravendb.client.documents.queries.Query;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.exceptions.RavenException;
import net.ravendb.client.infrastructure.DisabledOnPullRequest;
import net.ravendb.client.infrastructure.EnableOnServer;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.Duration;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisabledOnPullRequest
@EnableOnServer(thresholdVersion = "7.2")
public class SchemaValidationIndexingTests extends RemoteTestBase {

    private static final String INDEX_NAME = "IndexWithSchemaValidation";

    private static final String VALIDATE_NON_DOCUMENT_DATA_FOR_MAP_INDEX =
            "from doc in docs \n" +
            "select new \n" +
            "{\n" +
            "    Id = doc.Id,\n" +
            "    Errors = Schema.GetErrorsFor(doc.Inner)\n" +
            "}";

    private static final String VALIDATE_NON_DOCUMENT_DATA_FOR_JAVASCRIPT_MAP_INDEX =
            "map(\"TestObjs\", (doc) => { \n" +
            "    return {\n" +
            "        Id: id(doc),\n" +
            "        Errors: schema.getErrorsFor(doc.Inner)\n" +
            "    };\n" +
            "})";

    private static final String VALIDATE_DOCUMENT_DATA_FOR_MAP_INDEX =
            "from doc in docs\n" +
            "where MetadataFor(doc)[\"@collection\"] != \"@hilo\"\n" +
            "select new \n" +
            "{\n" +
            "    Id = doc.Id,\n" +
            "    Errors = Schema.GetErrorsFor(doc)\n" +
            "}";

    private static final String VALIDATE_DOCUMENT_DATA_FOR_JAVASCRIPT_MAP_INDEX =
            "map(\"@all_docs\", (doc) => { \n" +
            "    let metadata = doc[\"@metadata\"];\n" +
            "    if( metadata == null || metadata[\"@collection\"] !== \"@hilo\")\n" +
            "        return {\n" +
            "            Id: id(doc),\n" +
            "            Errors: schema.getErrorsFor(doc)\n" +
            "        };\n" +
            "})";

    private static final String MAX_LENGTH_ERROR = "The length of the value '0123456789a' at 'Prop' should not exceed 10, but its actual length is 11.";

    static Stream<Arguments> validateNonDocumentData() {
        return Stream.of(
                Arguments.of(VALIDATE_NON_DOCUMENT_DATA_FOR_MAP_INDEX, IndexType.MAP, "Schema.GetErrorsFor may only be called with a document"),
                Arguments.of(VALIDATE_NON_DOCUMENT_DATA_FOR_JAVASCRIPT_MAP_INDEX, IndexType.JAVA_SCRIPT_MAP, "'schema.GetErrorsFor' can only be performed on the source document.")
        );
    }

    static Stream<Arguments> validateDocumentData() {
        return Stream.of(
                Arguments.of(VALIDATE_DOCUMENT_DATA_FOR_MAP_INDEX, IndexType.MAP),
                Arguments.of(VALIDATE_DOCUMENT_DATA_FOR_JAVASCRIPT_MAP_INDEX, IndexType.JAVA_SCRIPT_MAP)
        );
    }

    private static String maxLengthSchema(String property) {
        return "{\"properties\":{\"" + property + "\":{\"maxLength\":10}}}";
    }

    private static IndexDefinition indexDefinition(String map, IndexType indexType, IndexSchemaDefinitions schemaDefinitions, String storedField) {
        IndexDefinition indexDefinition = new IndexDefinition();
        indexDefinition.setName(INDEX_NAME);
        indexDefinition.setMaps(Collections.singleton(map));
        indexDefinition.setSchemaDefinitions(schemaDefinitions);
        indexDefinition.setType(indexType);

        if (storedField != null) {
            IndexFieldOptions options = new IndexFieldOptions();
            options.setStorage(FieldStorage.YES);
            Map<String, IndexFieldOptions> fields = new HashMap<>();
            fields.put(storedField, options);
            indexDefinition.setFields(fields);
        }

        return indexDefinition;
    }

    private static IndexSchemaDefinitions schemaFor(String collection, String schema) {
        IndexSchemaDefinitions schemaDefinitions = new IndexSchemaDefinitions();
        schemaDefinitions.add(collection, schema);
        return schemaDefinitions;
    }

    private static List<IndexResult> queryResults(IDocumentSession session, Class<?> clazz) {
        return session.query(clazz, Query.index(INDEX_NAME)).selectFields(IndexResult.class).toList();
    }

    @ParameterizedTest
    @MethodSource("validateNonDocumentData")
    public void indexingSchemaErrors_WhenSchemaValidateNonDocument_ShouldFailIndexing(String map, IndexType indexType, String errorMsg) throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            String collection = store.getConventions().getFindCollectionName().apply(TestObj.class);

            store.maintenance().send(new PutIndexesOperation(
                    indexDefinition(map, indexType, schemaFor(collection, maxLengthSchema("Prop")), null)));

            try (BulkInsertOperation bulkInsert = store.bulkInsert()) {
                for (int i = 0; i < 1000; i++) {
                    TestObj obj = new TestObj("0123456789a");
                    Map<String, Object> inner = new HashMap<>();
                    inner.put("Prop", "0123456789a");
                    obj.setInner(inner);
                    bulkInsert.store(obj);
                }
            }

            IndexErrors[] indexErrors = waitForIndexingErrors(store, Duration.ofSeconds(15));
            assertThat(indexErrors).isNotEmpty();

            IndexingError[] errors = indexErrors[0].getErrors();
            assertThat(errors).isNotEmpty();
            assertThat(errors).anyMatch(error -> error.getError().contains(errorMsg));
        }
    }

    @ParameterizedTest
    @MethodSource("validateDocumentData")
    public void indexingSchemaErrors_WhenFailsOneRule_ShouldGetTheError(String map, IndexType indexType) throws Exception {
        String invalidDocId = "invalidDocId";
        String validDocId = "validDocId";

        try (IDocumentStore store = getDocumentStore()) {
            String collection = store.getConventions().getFindCollectionName().apply(TestObj.class);

            store.maintenance().send(new PutIndexesOperation(
                    indexDefinition(map, indexType, schemaFor(collection, maxLengthSchema("Prop")), "Errors")));

            try (IDocumentSession session = store.openSession()) {
                session.store(new TestObj("0123456789a"), invalidDocId);
                session.store(new TestObj("01"), validDocId);
                session.saveChanges();
            }

            waitForIndexing(store);

            try (IDocumentSession session = store.openSession()) {
                Map<String, IndexResult> results = queryResults(session, TestObj.class).stream()
                        .collect(Collectors.toMap(IndexResult::getId, Function.identity()));

                assertThat(results.get(validDocId).getErrors()).isNull();

                String[] errors = results.get(invalidDocId).getErrors();
                assertThat(errors).isNotNull();
                assertThat(errors).hasSize(1);
                assertThat(errors[0]).contains(MAX_LENGTH_ERROR);
            }
        }
    }

    @ParameterizedTest
    @MethodSource("validateDocumentData")
    public void indexingSchemaErrors_WhenFailsMultipleRules_ShouldGetTheErrors(String map, IndexType indexType) throws Exception {
        String[] expectedErrors = new String[] {
                MAX_LENGTH_ERROR,
                "The pattern of the value '0123456789a' at 'Prop' does not match the required pattern '^something'.",
                "The required property 'Prop1' is missing at ''."
        };

        try (IDocumentStore store = getDocumentStore()) {
            String collection = store.getConventions().getFindCollectionName().apply(TestObj.class);

            String schemaDefinition = "{\"properties\":{\"Prop\":{\"maxLength\":10,\"pattern\":\"^something\"}},\"required\":[\"Prop1\"]}";

            store.maintenance().send(new PutIndexesOperation(
                    indexDefinition(map, indexType, schemaFor(collection, schemaDefinition), "Errors")));

            try (IDocumentSession session = store.openSession()) {
                session.store(new TestObj("0123456789a"));
                session.store(new TestObj("0123456789a"));
                session.saveChanges();
            }

            waitForIndexing(store);

            try (IDocumentSession session = store.openSession()) {
                List<IndexResult> results = queryResults(session, TestObj.class);
                assertThat(results).isNotEmpty();
                for (IndexResult result : results) {
                    assertThat(result.getErrors()).isNotNull();
                    assertThat(result.getErrors()).containsExactlyInAnyOrder(expectedErrors);
                }
            }
        }
    }

    @ParameterizedTest
    @MethodSource("validateDocumentData")
    public void indexingSchemaErrors_WhenDefineSchemaOnMetadata_ShouldReject(String map, IndexType indexType) throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            String collection = store.getConventions().getFindCollectionName().apply(TestObj.class);

            IndexDefinition indexDefinition = indexDefinition(map, indexType,
                    schemaFor(collection, maxLengthSchema(Constants.Documents.Metadata.KEY)), "Error");

            assertThatThrownBy(() -> store.maintenance().send(new PutIndexesOperation(indexDefinition)))
                    .isInstanceOf(RavenException.class)
                    .hasMessageContaining("Define a schema validation on metadata is not allowed");
        }
    }

    @ParameterizedTest
    @MethodSource("validateDocumentData")
    public void indexingSchemaErrors_WhenSchemaDefinedInDatabase_ShouldIndexErrors(String map, IndexType indexType) throws Exception {
        String invalidDocId = "invalidDocId";
        String validDocId = "validDocId";

        try (IDocumentStore store = getDocumentStore()) {
            String collection = store.getConventions().getFindCollectionName().apply(TestObj.class);

            store.maintenance().send(new PutIndexesOperation(indexDefinition(map, indexType, null, "Errors")));

            try (IDocumentSession session = store.openSession()) {
                session.store(new TestObj("0123456789a"), invalidDocId);
                session.store(new TestObj("01"), validDocId);
                session.saveChanges();
            }

            SchemaDefinition schemaDefinition = new SchemaDefinition();
            schemaDefinition.setSchema(maxLengthSchema("Prop"));
            Map<String, SchemaDefinition> validators = new HashMap<>();
            validators.put(collection, schemaDefinition);
            SchemaValidationConfiguration configuration = new SchemaValidationConfiguration();
            configuration.setValidatorsPerCollection(validators);
            store.maintenance().send(new ConfigureSchemaValidationOperation(configuration));

            store.maintenance().send(new ResetIndexOperation(INDEX_NAME));
            waitForIndexing(store);

            try (IDocumentSession session = store.openSession()) {
                Map<String, IndexResult> results = queryResults(session, TestObj.class).stream()
                        .collect(Collectors.toMap(IndexResult::getId, Function.identity()));

                assertThat(results.get(validDocId).getErrors()).isNull();

                String[] errors = results.get(invalidDocId).getErrors();
                assertThat(errors).isNotNull();
                assertThat(errors).hasSize(1);
                assertThat(errors[0]).contains(MAX_LENGTH_ERROR);
            }
        }
    }

    @ParameterizedTest
    @MethodSource("validateDocumentData")
    public void indexingSchemaErrors_WhenNoSchemaDefined_ShouldBeAlwaysValid(String map, IndexType indexType) throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            store.maintenance().send(new PutIndexesOperation(indexDefinition(map, indexType, null, "Errors")));

            try (IDocumentSession session = store.openSession()) {
                session.store(new TestObj("0123456789a"));
                session.store(new TestObj("01"));
                session.saveChanges();
            }

            waitForIndexing(store);

            try (IDocumentSession session = store.openSession()) {
                // C#: from x in Query<TestObj>(index).As<IndexResult>() let errors = x.Errors select new { Errors = errors }
                List<IndexResult> results = session.advanced()
                        .rawQuery(IndexResult.class, "from index '" + INDEX_NAME + "' as x select { Errors : x.Errors }")
                        .toList();
                assertThat(results).isNotEmpty();
                for (IndexResult result : results) {
                    assertThat(result.getErrors()).isNotNull();
                    assertThat(result.getErrors()).isEmpty();
                }
            }
        }
    }

    @ParameterizedTest
    @MethodSource("validateDocumentData")
    public void indexingSchemaErrors_WhenDefineOnEmptyCollection_ShouldGetTheError(String map, IndexType indexType) throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            store.maintenance().send(new PutIndexesOperation(indexDefinition(map, indexType,
                    schemaFor(Constants.Documents.Collections.EMPTY_COLLECTION, maxLengthSchema("Prop")), "Errors")));

            // C# stores an anonymous object, which has no collection - put a raw document without @collection
            ObjectNode doc = store.getConventions().getEntityMapper().createObjectNode();
            doc.put("Prop", "0123456789a");
            store.getRequestExecutor().execute(new PutDocumentCommand(store.getConventions(), "docs/1", null, doc));

            waitForIndexing(store);

            try (IDocumentSession session = store.openSession()) {
                List<IndexResult> results = queryResults(session, Object.class);
                assertThat(results).hasSize(1);

                String[] errors = results.get(0).getErrors();
                assertThat(errors).isNotNull();
                assertThat(errors).hasSize(1);
                assertThat(errors[0]).contains(MAX_LENGTH_ERROR);
            }
        }
    }

    public static class TestObj {
        private String id;

        @JsonProperty("Prop")
        private String prop;

        @JsonProperty("Inner")
        private Object inner;

        public TestObj() {
        }

        public TestObj(String prop) {
            this.prop = prop;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getProp() {
            return prop;
        }

        public void setProp(String prop) {
            this.prop = prop;
        }

        public Object getInner() {
            return inner;
        }

        public void setInner(Object inner) {
            this.inner = inner;
        }
    }

    public static class IndexResult {
        private String id;

        @JsonProperty("Errors")
        private String[] errors;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String[] getErrors() {
            return errors;
        }

        public void setErrors(String[] errors) {
            this.errors = errors;
        }
    }
}
