package net.ravendb.client.documents.operations.schemaValidation;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.node.ObjectNode;
import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.operations.IMaintenanceOperation;
import net.ravendb.client.documents.operations.OperationIdResult;
import net.ravendb.client.documents.operations.OperationIdResultGeneric;
import net.ravendb.client.http.IRaftCommand;
import net.ravendb.client.http.RavenCommand;
import net.ravendb.client.http.ServerNode;
import net.ravendb.client.json.ContentProviderHttpEntity;
import net.ravendb.client.util.RaftIdGenerator;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.core5.http.ContentType;
import java.io.IOException;

public final class StartSchemaValidationOperation
        implements IMaintenanceOperation<OperationIdResultGeneric<StartValidateSchemaOperationResult>> {

    private final Parameters parameters;

    public static final class Parameters {

        private String schemaDefinition;
        private String collection;
        private Integer maxErrorMessages;
        private Long maxDocumentsToValidate;
        private Long startEtag;

        public String getSchemaDefinition() {
            return schemaDefinition;
        }

        public void setSchemaDefinition(String schemaDefinition) {
            this.schemaDefinition = schemaDefinition;
        }

        public String getCollection() {
            return collection;
        }

        public void setCollection(String collection) {
            this.collection = collection;
        }

        public Integer getMaxErrorMessages() {
            return maxErrorMessages;
        }

        public void setMaxErrorMessages(Integer maxErrorMessages) {
            this.maxErrorMessages = maxErrorMessages;
        }

        public Long getMaxDocumentsToValidate() {
            return maxDocumentsToValidate;
        }

        public void setMaxDocumentsToValidate(Long maxDocumentsToValidate) {
            this.maxDocumentsToValidate = maxDocumentsToValidate;
        }

        public Long getStartEtag() {
            return startEtag;
        }

        public void setStartEtag(Long startEtag) {
            this.startEtag = startEtag;
        }
    }

    public StartSchemaValidationOperation(Parameters parameters) {
        if (parameters == null) {
            throw new IllegalArgumentException("parameters cannot be null");
        }

        if (parameters.getSchemaDefinition() == null ||
                parameters.getSchemaDefinition().trim().isEmpty()) {
            throw new IllegalArgumentException("Schema must be provided.");
        }

        if (parameters.getCollection() == null ||
                parameters.getCollection().trim().isEmpty()) {
            throw new IllegalArgumentException("Collection must be provided.");
        }

        if (parameters.getMaxErrorMessages() != null &&
                parameters.getMaxErrorMessages() < 0) {
            throw new IllegalArgumentException("MaxErrorMessages must be >= 0.");
        }

        if (parameters.getMaxDocumentsToValidate() != null &&
                parameters.getMaxDocumentsToValidate() <= 0) {
            throw new IllegalArgumentException("MaxDocumentsToValidate must be > 0.");
        }

        this.parameters = parameters;
    }

    @Override
    public RavenCommand<OperationIdResultGeneric<StartValidateSchemaOperationResult>> getCommand(
            DocumentConventions conventions) {
        return new StartSchemaValidationCommand(conventions, parameters);
    }

    public static class StartSchemaValidationCommand
            extends RavenCommand<OperationIdResultGeneric<StartValidateSchemaOperationResult>>
            implements IRaftCommand {

        private final DocumentConventions conventions;
        private final Parameters parameters;
        private final Long operationId;

        @SuppressWarnings("unchecked")
        public StartSchemaValidationCommand(
                DocumentConventions conventions,
                Parameters parameters,
                Long operationId) {
            super((Class<OperationIdResultGeneric<StartValidateSchemaOperationResult>>) (Class<?>) OperationIdResultGeneric.class);

            if (conventions == null) {
                throw new IllegalArgumentException("conventions cannot be null");
            }
            if (parameters == null) {
                throw new IllegalArgumentException("parameters cannot be null");
            }

            this.conventions = conventions;
            this.parameters = parameters;
            this.operationId = operationId;
        }

        public StartSchemaValidationCommand(
                DocumentConventions conventions,
                Parameters parameters) {
            this(conventions, parameters, null);
        }

        @Override
        public boolean isReadRequest() {
            return false;
        }

        @Override
        public HttpUriRequestBase createRequest(ServerNode node) {
            String url = node.getUrl()
                    + "/databases/" + node.getDatabase()
                    + "/schema-validation/validate";

            if (operationId != null) {
                url += "?operationId=" + operationId;
            }

            HttpPost request = new HttpPost(url);

            request.setEntity(new ContentProviderHttpEntity(outputStream -> {
                try (JsonGenerator generator = createSafeJsonGenerator(outputStream)) {
                    ObjectNode json = mapper.valueToTree(parameters);
                    generator.writeTree(json);
                }
            }, ContentType.APPLICATION_JSON, conventions));

            return request;
        }


        @Override
        public void setResponse(String response, boolean fromCache) throws IOException {
            if (response == null) {
                throwInvalidResponse();
            }

            result = mapper.readValue(response, resultClass);
        }


        @Override
        public String getRaftUniqueRequestId() {
            return RaftIdGenerator.newId();
        }
    }
}

