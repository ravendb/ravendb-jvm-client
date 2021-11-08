package net.ravendb.client.serverwide.operations;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.node.ObjectNode;
import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.operations.revisions.RevisionsCollectionConfiguration;
import net.ravendb.client.http.IRaftCommand;
import net.ravendb.client.http.RavenCommand;
import net.ravendb.client.http.ServerNode;
import net.ravendb.client.json.ContentProviderHttpEntity;
import net.ravendb.client.primitives.Reference;
import net.ravendb.client.util.RaftIdGenerator;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.ContentType;

import java.io.IOException;

public class ConfigureRevisionsForConflictsOperation implements IServerOperation<ConfigureRevisionsForConflictsResult> {

    private final String _database;
    private final RevisionsCollectionConfiguration _configuration;

    public ConfigureRevisionsForConflictsOperation(String database, RevisionsCollectionConfiguration configuration) {
        _database = database;
        if (configuration == null) {
            throw new IllegalArgumentException("Configuration cannot be null");
        }
        _configuration = configuration;
    }

    @Override
    public RavenCommand<ConfigureRevisionsForConflictsResult> getCommand(DocumentConventions conventions) {
        return new ConfigureRevisionsForConflictsCommand(conventions, _database, _configuration);
    }

    private static class ConfigureRevisionsForConflictsCommand extends RavenCommand<ConfigureRevisionsForConflictsResult> implements IRaftCommand {
        private final DocumentConventions _conventions;
        private final String _databaseName;
        private final RevisionsCollectionConfiguration _configuration;

        public ConfigureRevisionsForConflictsCommand(DocumentConventions conventions, String database, RevisionsCollectionConfiguration configuration) {
            super(ConfigureRevisionsForConflictsResult.class);

            if (conventions == null) {
                throw new IllegalArgumentException("Conventions cannot be null");
            }

            _conventions = conventions;

            if (database == null) {
                throw new IllegalArgumentException("Database cannot be null");
            }

            _databaseName = database;
            _configuration = configuration;
        }

        @Override
        public HttpRequestBase createRequest(ServerNode node, Reference<String> url) {
            url.value = node.getUrl() + "/databases/" + _databaseName + "/admin/revisions/conflicts/config";

            HttpPost request = new HttpPost();
            request.setEntity(new ContentProviderHttpEntity(outputStream -> {
                try (JsonGenerator generator = createSafeJsonGenerator(outputStream)) {
                    ObjectNode config = mapper.valueToTree(_configuration);
                    generator.writeTree(config);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }, ContentType.APPLICATION_JSON));
            return request;
        }

        @Override
        public void setResponse(String response, boolean fromCache) throws IOException {
            if (response == null) {
                throwInvalidResponse();
            }

            result = mapper.readValue(response, ConfigureRevisionsForConflictsResult.class);
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
}
