package net.ravendb.client.serverwide.operations;

import com.fasterxml.jackson.core.JsonGenerator;
import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.http.IRaftCommand;
import net.ravendb.client.http.RavenCommand;
import net.ravendb.client.http.ServerNode;
import net.ravendb.client.json.ContentProviderHttpEntity;
import net.ravendb.client.serverwide.DatabaseTopology;
import net.ravendb.client.util.ClientShardHelper;
import net.ravendb.client.util.RaftIdGenerator;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.core5.http.ContentType;

import java.io.IOException;

public class ModifyDatabaseTopologyOperation implements IServerOperation<ModifyDatabaseTopologyResult> {
    private String _databaseName;
    private final DatabaseTopology _databaseTopology;

    public ModifyDatabaseTopologyOperation(String databaseName, DatabaseTopology databaseTopology) {
        if (databaseTopology == null) {
            throw new IllegalArgumentException("DatabaseTopology cannot be null");
        }
        _databaseTopology = databaseTopology;
        _databaseName = databaseName;
    }

    public ModifyDatabaseTopologyOperation(String databaseName, int shardNumber, DatabaseTopology databaseTopology) {
        this(databaseName, databaseTopology);

        _databaseName = ClientShardHelper.toShardName(databaseName, shardNumber);
    }

    @Override
    public RavenCommand<ModifyDatabaseTopologyResult> getCommand(DocumentConventions conventions) {
        return new ModifyDatabaseTopologyCommand(conventions, _databaseName, _databaseTopology);
    }

    private static class ModifyDatabaseTopologyCommand extends RavenCommand<ModifyDatabaseTopologyResult> implements IRaftCommand {
        private final DocumentConventions _conventions;
        private final String _databaseName;
        private final DatabaseTopology _databaseTopology;

        public ModifyDatabaseTopologyCommand(DocumentConventions conventions, String databaseName, DatabaseTopology databaseTopology) {
            super(ModifyDatabaseTopologyResult.class);

            if (databaseTopology == null) {
                throw new IllegalArgumentException("DatabaseTopology cannot be null");
            }
            _conventions = conventions;
            _databaseTopology = databaseTopology;
            _databaseName = databaseName;
        }

        @Override
        public HttpUriRequestBase createRequest(ServerNode node) {
            String url = node.getUrl() + "/admin/databases/topology/modify?name=" + _databaseName;

            HttpPost request = new HttpPost(url);
            request.setEntity(new ContentProviderHttpEntity(outputStream -> {
                try (JsonGenerator generator = createSafeJsonGenerator(outputStream)) {
                    generator.getCodec().writeValue(generator, _databaseTopology);
                }
            }, ContentType.APPLICATION_JSON, _conventions));

            return request;
        }

        @Override
        public void setResponse(String response, boolean fromCache) throws IOException {
            if (response == null) {
                throwInvalidResponse();
            }

            result = mapper.readValue(response, ModifyDatabaseTopologyResult.class);
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
