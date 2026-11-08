package net.ravendb.client.test.schemaValidation;

import com.fasterxml.jackson.annotation.JsonProperty;
import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.BulkInsertOperation;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.operations.Operation;
import net.ravendb.client.documents.operations.OperationIdResultGeneric;
import net.ravendb.client.documents.operations.schemaValidation.StartSchemaValidationOperation;
import net.ravendb.client.documents.operations.schemaValidation.StartValidateSchemaOperationResult;
import net.ravendb.client.documents.operations.schemaValidation.ValidateSchemaResult;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.infrastructure.DisabledOnPullRequest;
import net.ravendb.client.infrastructure.EnableOnServer;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisabledOnPullRequest
@EnableOnServer(thresholdVersion = "7.2")
public class SchemaValidationOperationTests extends RemoteTestBase {

    private static String maxLengthSchema(int maxLength) {
        return "{\"properties\":{\"Prop\":{\"maxLength\":" + maxLength + "}}}";
    }

    private static StartSchemaValidationOperation.Parameters parameters(String schemaDefinition) {
        StartSchemaValidationOperation.Parameters parameters = new StartSchemaValidationOperation.Parameters();
        parameters.setSchemaDefinition(schemaDefinition);
        parameters.setCollection("TestObjs");
        return parameters;
    }

    @Test
    public void validateSchema_WhenDone_ShouldAbleToFetchResult() throws Exception {
        String invalidDocId = "invalidDocId";
        String validDocId = "validDocId";

        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                session.store(new TestObj("0123456789a"), invalidDocId);
                session.store(new TestObj("01"), validDocId);
                session.saveChanges();
            }

            Operation operation = store.maintenance().sendAsync(new StartSchemaValidationOperation(parameters(maxLengthSchema(10))));
            ValidateSchemaResult result = operation.waitForCompletion(ValidateSchemaResult.class);

            assertThat(result.getErrorCount()).isEqualTo(1);
            assertThat(result.getValidatedCount()).isEqualTo(2);
            assertThat(result.getErrors()).hasSize(1);
            assertThat(result.getErrors()).containsKey(invalidDocId);
            assertThat(result.getErrors().get(invalidDocId))
                    .contains("The length of the value '0123456789a' at 'Prop' should not exceed 10, but its actual length is 11.");
        }
    }

    @Test
    public void validateSchema_SendReturnsOperationIdAndResult() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                session.store(new TestObj("01"), "validDocId");
                session.saveChanges();
            }

            OperationIdResultGeneric<StartValidateSchemaOperationResult> result =
                    store.maintenance().send(new StartSchemaValidationOperation(parameters(maxLengthSchema(10))));

            assertThat(result.getResult()).isNotNull();
            assertThat(result.getResult().getOperationId()).isEqualTo(result.getOperationId());
            assertThat(result.getOperationNodeTag()).isNotNull();
        }
    }

    @Test
    public void validateSchema_WhenLimitErrors_ShouldGetOnlyTheRequiredLimit() throws Exception {
        int errorDocumentCount = 10000;
        int maxErrorsMsg = 10;

        try (IDocumentStore store = getDocumentStore()) {
            try (BulkInsertOperation bulkInsert = store.bulkInsert()) {
                for (int i = 0; i < errorDocumentCount; i++) {
                    bulkInsert.store(new TestObj("0123456789a"));
                }
            }

            StartSchemaValidationOperation.Parameters parameters = parameters(maxLengthSchema(10));
            parameters.setMaxErrorMessages(maxErrorsMsg);

            Operation operation = store.maintenance().sendAsync(new StartSchemaValidationOperation(parameters));
            ValidateSchemaResult result = operation.waitForCompletion(ValidateSchemaResult.class);

            assertThat(result.getErrorCount()).isEqualTo(errorDocumentCount);
            assertThat(result.getValidatedCount()).isEqualTo(errorDocumentCount);
            assertThat(result.getErrors()).hasSize(maxErrorsMsg);
            for (String error : result.getErrors().values()) {
                assertThat(error).contains("The length of the value '0123456789a' at 'Prop' should not exceed 10, but its actual length is 11.");
            }
        }
    }

    @Test
    public void validateSchemaOperation_WhenSettingEtagOnNonSharded_ShouldStartFromTheEtag() throws Exception {
        String id1 = "user/1";
        String id2 = "user/2";

        try (IDocumentStore store = getDocumentStore()) {
            try (BulkInsertOperation bulkInsert = store.bulkInsert()) {
                bulkInsert.store(new TestObj("0123456789a"), id1);
                bulkInsert.store(new TestObj("0123456789ab"), id2);
            }

            StartSchemaValidationOperation.Parameters parameters1 = parameters(maxLengthSchema(10));
            parameters1.setMaxDocumentsToValidate(1L);

            Operation operation1 = store.maintenance().sendAsync(new StartSchemaValidationOperation(parameters1));
            ValidateSchemaResult result1 = operation1.waitForCompletion(ValidateSchemaResult.class);
            Map.Entry<String, String> error1 = result1.getErrors().entrySet().iterator().next();
            assertThat(error1.getKey()).isEqualTo(id1);
            assertThat(error1.getValue()).startsWith("The length of the value '0123456789a' at 'Prop' should not exceed 10, but its actual length is 11.");

            StartSchemaValidationOperation.Parameters parameters2 = parameters(maxLengthSchema(10));
            parameters2.setStartEtag(result1.getLastEtag() + 1);

            Operation operation2 = store.maintenance().sendAsync(new StartSchemaValidationOperation(parameters2));
            ValidateSchemaResult result2 = operation2.waitForCompletion(ValidateSchemaResult.class);
            Map.Entry<String, String> error2 = result2.getErrors().entrySet().iterator().next();
            assertThat(error2.getKey()).isEqualTo(id2);
            assertThat(error2.getValue()).startsWith("The length of the value '0123456789ab' at 'Prop' should not exceed 10, but its actual length is 12.");
        }
    }

    @Test
    public void startSchemaValidationOperation_ValidatesParameters() {
        StartSchemaValidationOperation.Parameters negativeErrors = parameters(maxLengthSchema(10));
        negativeErrors.setMaxErrorMessages(-1);
        assertThatThrownBy(() -> new StartSchemaValidationOperation(negativeErrors))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Property MaxErrorMessages must be >= 0.");

        StartSchemaValidationOperation.Parameters zeroDocs = parameters(maxLengthSchema(10));
        zeroDocs.setMaxDocumentsToValidate(0L);
        assertThatThrownBy(() -> new StartSchemaValidationOperation(zeroDocs))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Property MaxDocumentsToValidate must be > 0.");
    }

    public static class TestObj {
        private String id;

        @JsonProperty("Prop")
        private String prop;

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
    }
}
