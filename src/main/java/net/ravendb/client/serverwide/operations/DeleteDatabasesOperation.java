package net.ravendb.client.serverwide.operations;

import com.fasterxml.jackson.core.JsonProcessingException;
import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.http.IRaftCommand;
import net.ravendb.client.http.RavenCommand;
import net.ravendb.client.http.ServerNode;
import net.ravendb.client.primitives.ExceptionsUtils;
import net.ravendb.client.util.ClientShardHelper;
import net.ravendb.client.util.RaftIdGenerator;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.StringEntity;

import java.io.IOException;
import java.time.Duration;

public class DeleteDatabasesOperation implements IServerOperation<DeleteDatabaseResult> {

    private final Parameters parameters;

    public DeleteDatabasesOperation(String databaseName, boolean hardDelete) {
        this(databaseName, hardDelete, null, null);
    }

    public DeleteDatabasesOperation(String databaseName, boolean hardDelete, String fromNode) {
        this(databaseName, hardDelete, fromNode, null);
    }

    public DeleteDatabasesOperation(String databaseName, boolean hardDelete, String fromNode, Duration timeToWaitForConfirmation) {
        if (databaseName == null) {
            throw new IllegalArgumentException("Database name cannot be null");
        }

        Parameters parameters = new Parameters();
        parameters.setDatabaseNames(new String[] { databaseName });
        parameters.setHardDelete(hardDelete);
        parameters.setTimeToWaitForConfirmation(timeToWaitForConfirmation);

        if (fromNode != null) {
            parameters.setFromNodes(new String[] { fromNode });
        }

        this.parameters = parameters;
    }

    public DeleteDatabasesOperation(String databaseName, int shardNumber, boolean hardDelete, String fromNode, Duration timeToWaitForConfirmation) {
        this(ClientShardHelper.toShardName(databaseName, shardNumber), hardDelete, fromNode, timeToWaitForConfirmation);
    }

    public DeleteDatabasesOperation(Parameters parameters) {
        if (parameters == null) {
            throw new IllegalArgumentException("Parameters cannot be null");
        }

        if (parameters.getDatabaseNames() == null || parameters.getDatabaseNames().length == 0) {
            throw new IllegalArgumentException("Database names cannot be null");
        }

        this.parameters = parameters;
    }

    @Override
    public RavenCommand<DeleteDatabaseResult> getCommand(DocumentConventions conventions) {
        return new DeleteDatabaseCommand(conventions, this.parameters);
    }

    private static class DeleteDatabaseCommand extends RavenCommand<DeleteDatabaseResult> implements IRaftCommand {
        private final String parameters;

        public DeleteDatabaseCommand(DocumentConventions conventions, Parameters parameters) {
            super(DeleteDatabaseResult.class);

            if (conventions == null) {
                throw new IllegalArgumentException("Conventions cannot be null");
            }

            if (parameters == null) {
                throw new IllegalArgumentException("Parameters cannot be null");
            }

            try {
                this.parameters = mapper.writeValueAsString(parameters);
            } catch (JsonProcessingException e) {
                throw ExceptionsUtils.unwrapException(e);
            }
        }

        @Override
        public HttpUriRequestBase createRequest(ServerNode node) {
            String url = node.getUrl() + "/admin/databases";

            HttpDeleteWithEntity request = new HttpDeleteWithEntity(url);

            request.setEntity(new StringEntity(this.parameters, ContentType.APPLICATION_JSON));

            return request;
        }

        @Override
        public void setResponse(String response, boolean fromCache) throws IOException {
            result = mapper.readValue(response, resultClass);
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
        private boolean hardDelete;
        private String[] fromNodes;
        private Duration timeToWaitForConfirmation;

        public String[] getDatabaseNames() {
            return databaseNames;
        }

        public void setDatabaseNames(String[] databaseNames) {
            this.databaseNames = databaseNames;
        }

        public boolean isHardDelete() {
            return hardDelete;
        }

        public void setHardDelete(boolean hardDelete) {
            this.hardDelete = hardDelete;
        }

        public String[] getFromNodes() {
            return fromNodes;
        }

        public void setFromNodes(String[] fromNodes) {
            this.fromNodes = fromNodes;
        }

        public Duration getTimeToWaitForConfirmation() {
            return timeToWaitForConfirmation;
        }

        public void setTimeToWaitForConfirmation(Duration timeToWaitForConfirmation) {
            this.timeToWaitForConfirmation = timeToWaitForConfirmation;
        }
    }
}
