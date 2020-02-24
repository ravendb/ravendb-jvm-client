package net.ravendb.client.serverwide.operations;

import com.fasterxml.jackson.core.JsonGenerator;
import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.http.IRaftCommand;
import net.ravendb.client.http.ServerNode;
import net.ravendb.client.http.VoidRavenCommand;
import net.ravendb.client.json.ContentProviderHttpEntity;
import net.ravendb.client.primitives.Reference;
import net.ravendb.client.util.RaftIdGenerator;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.ContentType;

import java.io.IOException;
import java.util.Set;

public class UpdateUnusedDatabasesOperation implements IVoidServerOperation {

    private final String _database;
    private final Parameters _parameters;

    public UpdateUnusedDatabasesOperation(String database, Set<String> unusedDatabaseIds) {
        if (StringUtils.isEmpty(database)) {
            throw new IllegalArgumentException("Database cannot be null");
        }

        _database = database;
        _parameters = new Parameters();
        _parameters.setDatabaseIds(unusedDatabaseIds);
    }

    @Override
    public VoidRavenCommand getCommand(DocumentConventions conventions) {
        return new UpdateUnusedDatabasesCommand(_database, _parameters);
    }

    private static class UpdateUnusedDatabasesCommand extends VoidRavenCommand implements IRaftCommand {
        private final String _database;
        private final Parameters _parameters;

        public UpdateUnusedDatabasesCommand(String database, Parameters parameters) {
            _database = database;
            _parameters = parameters;
        }

        @Override
        public HttpRequestBase createRequest(ServerNode node, Reference<String> url) {
            url.value = node.getUrl() + "/admin/databases/unused-ids?name=" + _database;

            HttpPost request = new HttpPost();
            request.setEntity(new ContentProviderHttpEntity(outputStream -> {
                try (JsonGenerator generator = mapper.getFactory().createGenerator(outputStream)) {
                    generator.getCodec().writeValue(generator, _parameters);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }, ContentType.APPLICATION_JSON));

            return request;
        }

        @Override
        public String getRaftUniqueRequestId() {
            return RaftIdGenerator.newId();
        }
    }

    public static class Parameters {
        private Set<String> databaseIds;

        public Set<String> getDatabaseIds() {
            return databaseIds;
        }

        public void setDatabaseIds(Set<String> databaseIds) {
            this.databaseIds = databaseIds;
        }
    }
}
