package net.ravendb.client.serverwide.operations.ongoingTasks;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.node.ObjectNode;
import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.http.IRaftCommand;
import net.ravendb.client.http.ServerNode;
import net.ravendb.client.http.VoidRavenCommand;
import net.ravendb.client.json.ContentProviderHttpEntity;
import net.ravendb.client.primitives.Reference;
import net.ravendb.client.serverwide.DatabaseRecord;
import net.ravendb.client.serverwide.operations.IVoidServerOperation;
import net.ravendb.client.util.RaftIdGenerator;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.ContentType;

import java.io.IOException;

public class SetDatabasesLockOperation implements IVoidServerOperation {

    private final Parameters _parameters;

    public SetDatabasesLockOperation(String databaseName, DatabaseRecord.DatabaseLockMode mode) {
        if (databaseName == null) {
            throw new IllegalArgumentException("DatabaseName cannot be null");
        }

        _parameters = new Parameters();
        _parameters.setDatabaseNames(new String[]{ databaseName });
        _parameters.setMode(mode);
    }

    public SetDatabasesLockOperation(Parameters parameters) {
        if (parameters == null) {
            throw new IllegalArgumentException("Parameters cannot be null");
        }

        if (parameters.getDatabaseNames() == null || parameters.getDatabaseNames().length == 0) {
            throw new IllegalArgumentException("DatabaseNames cannot be null or empty");
        }

        _parameters = parameters;
    }

    @Override
    public VoidRavenCommand getCommand(DocumentConventions conventions) {
        return new SetDatabasesLockCommand(conventions, _parameters);
    }

    private static class SetDatabasesLockCommand extends VoidRavenCommand implements IRaftCommand {
        private final ObjectNode _parameters;

        public SetDatabasesLockCommand(DocumentConventions conventions, Parameters parameters) {
            if (conventions == null) {
                throw new IllegalArgumentException("Conventions cannot be null");
            }

            if (parameters == null) {
                throw new IllegalArgumentException("Parameters cannot be null");
            }

            _parameters = mapper.valueToTree(parameters);
        }

        @Override
        public HttpRequestBase createRequest(ServerNode node, Reference<String> url) {
            url.value = node.getUrl() + "/admin/databases/set-lock";

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
        private String[] databaseNames;
        private DatabaseRecord.DatabaseLockMode mode;

        public String[] getDatabaseNames() {
            return databaseNames;
        }

        public void setDatabaseNames(String[] databaseNames) {
            this.databaseNames = databaseNames;
        }

        public DatabaseRecord.DatabaseLockMode getMode() {
            return mode;
        }

        public void setMode(DatabaseRecord.DatabaseLockMode mode) {
            this.mode = mode;
        }
    }
}
