package net.ravendb.client.serverwide.operations;

import com.fasterxml.jackson.core.JsonGenerator;
import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.http.IRaftCommand;
import net.ravendb.client.http.ServerNode;
import net.ravendb.client.http.VoidRavenCommand;
import net.ravendb.client.json.ContentProviderHttpEntity;
import net.ravendb.client.util.RaftIdGenerator;
import org.apache.commons.lang3.StringUtils;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.core5.http.ContentType;

import java.util.Set;

public class UpdateUnusedDatabasesOperation implements IVoidServerOperation {

    private final String _database;
    private final Parameters _parameters;

    public UpdateUnusedDatabasesOperation(String database, Set<String> unusedDatabaseIds) {
        this(database, unusedDatabaseIds, false);
    }

    public UpdateUnusedDatabasesOperation(String database, Set<String> unusedDatabaseIds, boolean validate) {
        if (StringUtils.isEmpty(database)) {
            throw new IllegalArgumentException("Database cannot be null");
        }

        _database = database;
        _parameters = new Parameters();
        _parameters.setDatabaseIds(unusedDatabaseIds);
        _parameters.setValidate(validate);
    }

    @Override
    public VoidRavenCommand getCommand(DocumentConventions conventions) {
        return new UpdateUnusedDatabasesCommand(conventions, _database, _parameters);
    }

    private static class UpdateUnusedDatabasesCommand extends VoidRavenCommand implements IRaftCommand {
        private final String _database;
        private final Parameters _parameters;
        private final DocumentConventions _conventions;

        public UpdateUnusedDatabasesCommand(DocumentConventions conventions, String database, Parameters parameters) {
            _database = database;
            _parameters = parameters;
            _conventions = conventions;
        }

        @Override
        public HttpUriRequestBase createRequest(ServerNode node) {
            String url = node.getUrl() + "/admin/databases/unused-ids?name=" + _database;
            if (_parameters.validate) {
                url += "&validate=true";
            }

            HttpPost request = new HttpPost(url);
            request.setEntity(new ContentProviderHttpEntity(outputStream -> {
                try (JsonGenerator generator = createSafeJsonGenerator(outputStream)) {
                    generator.getCodec().writeValue(generator, _parameters);
                }
            }, ContentType.APPLICATION_JSON, _conventions));

            return request;
        }

        @Override
        public String getRaftUniqueRequestId() {
            return RaftIdGenerator.newId();
        }
    }

    public static class Parameters {
        private Set<String> databaseIds;
        private boolean validate;

        public Set<String> getDatabaseIds() {
            return databaseIds;
        }

        public void setDatabaseIds(Set<String> databaseIds) {
            this.databaseIds = databaseIds;
        }

        public boolean isValidate() {
            return validate;
        }

        public void setValidate(boolean validate) {
            this.validate = validate;
        }
    }
}
