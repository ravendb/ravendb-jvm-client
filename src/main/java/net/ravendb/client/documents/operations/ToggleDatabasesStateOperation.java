package net.ravendb.client.documents.operations;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.http.IRaftCommand;
import net.ravendb.client.http.RavenCommand;
import net.ravendb.client.http.ServerNode;
import net.ravendb.client.json.ContentProviderHttpEntity;
import net.ravendb.client.serverwide.operations.IServerOperation;
import net.ravendb.client.util.RaftIdGenerator;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.core5.http.ContentType;

import java.io.IOException;

public class ToggleDatabasesStateOperation implements IServerOperation<DisableDatabaseToggleResult> {

    private final boolean _disable;
    private final Parameters _parameters;

    public ToggleDatabasesStateOperation(String databaseName, boolean disable) {
        if (databaseName == null) {
            throw new IllegalArgumentException("DatabaseName cannot be null");
        }

        _disable = disable;
        _parameters = new Parameters();
        _parameters.setDatabaseNames(new String[] { databaseName });
    }

    public ToggleDatabasesStateOperation(String[] databaseNames, boolean disable) {
        _disable = disable;
        _parameters = new Parameters(databaseNames);
    }

    public ToggleDatabasesStateOperation(Parameters parameters, boolean disable) {
        if (parameters == null) {
            throw new IllegalArgumentException("Parameters cannot be null");
        }

        if (parameters.getDatabaseNames() == null || parameters.getDatabaseNames().length == 0) {
            throw new IllegalArgumentException("Parameters.DatabaseNames cannot be null or empty");
        }
        _disable = disable;
        _parameters = parameters;
    }

    @Override
    public RavenCommand<DisableDatabaseToggleResult> getCommand(DocumentConventions conventions) {
        return new ToggleDatabaseStateCommand(conventions, _parameters, _disable);
    }

    private static class ToggleDatabaseStateCommand extends RavenCommand<DisableDatabaseToggleResult> implements IRaftCommand {
        private final DocumentConventions _conventions;
        private final boolean _disable;
        private final Parameters _parameters;

        public ToggleDatabaseStateCommand(DocumentConventions conventions, Parameters parameters, boolean disable) {
            super(DisableDatabaseToggleResult.class);

            if (parameters == null) {
                throw new IllegalArgumentException("Parameters cannot be null");
            }

            _conventions = conventions;
            _disable = disable;
            _parameters = parameters;
        }

        @Override
        public HttpUriRequestBase createRequest(ServerNode node) {
            String toggle = _disable ? "disable" : "enable";
            String url = node.getUrl() + "/admin/databases/" + toggle;

            HttpPost request = new HttpPost(url);
            request.setEntity(new ContentProviderHttpEntity(outputStream -> {
                try (JsonGenerator generator = createSafeJsonGenerator(outputStream)) {
                    ObjectNode config = mapper.valueToTree(_parameters);
                    generator.writeTree(config);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }, ContentType.APPLICATION_JSON, _conventions));
            return request;
        }

        @Override
        public void setResponse(String response, boolean fromCache) throws IOException {
            if (response == null) {
                throwInvalidResponse();
            }

            JsonNode jsonNode = mapper.readTree(response);
            JsonNode statusNode = jsonNode.get("Status");

            if (!statusNode.isArray()) {
                throwInvalidResponse();
            }

            JsonNode databaseStatus = ((ArrayNode)statusNode).get(0);
            result = mapper.treeToValue(databaseStatus, resultClass);
        }

        @Override
        public boolean isReadRequest() {
            return false;
        }

        @Override
        public String getRaftUniqueRequestId() {
            return RaftIdGenerator.newId();
        }
    }


    public static class Parameters {
        private String[] databaseNames;

        public Parameters() {
        }

        public Parameters(String[] databaseNames) {
            this.databaseNames = databaseNames;
        }

        public String[] getDatabaseNames() {
            return databaseNames;
        }

        public void setDatabaseNames(String[] databaseNames) {
            this.databaseNames = databaseNames;
        }
    }

}
