package net.ravendb.client.serverwide.operations;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.node.ObjectNode;
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

import java.util.List;

public class ReorderDatabaseMembersOperation implements IVoidServerOperation {

    public static class Parameters {
        private List<String> membersOrder;
        private boolean fixed;

        public boolean isFixed() {
            return fixed;
        }

        public void setFixed(boolean fixed) {
            this.fixed = fixed;
        }

        public List<String> getMembersOrder() {
            return membersOrder;
        }

        public void setMembersOrder(List<String> membersOrder) {
            this.membersOrder = membersOrder;
        }
    }

    private final String _database;
    private final Parameters _parameters;


    public ReorderDatabaseMembersOperation(String database, List<String> order) {
        this(database, order, false);
    }

    public ReorderDatabaseMembersOperation(String database, List<String> order, boolean fixed) {
        if (order == null || order.isEmpty()) {
            throw new IllegalArgumentException("Order list must contain values");
        }

        _database = database;
        Parameters parameters = new Parameters();
        parameters.setMembersOrder(order);
        parameters.setFixed(fixed);
        _parameters = parameters;
    }

    @Override
    public VoidRavenCommand getCommand(DocumentConventions conventions) {
        return new ReorderDatabaseMembersCommand(conventions, _database, _parameters);
    }

    private static class ReorderDatabaseMembersCommand extends VoidRavenCommand implements IRaftCommand {
        private final DocumentConventions _conventions;

        private final String _databaseName;
        private final Parameters _parameters;

        public ReorderDatabaseMembersCommand(DocumentConventions conventions, String databaseName, Parameters parameters) {
            if (StringUtils.isEmpty(databaseName)) {
                throw new IllegalArgumentException("Database cannot be empty");
            }

            _conventions = conventions;
            _databaseName = databaseName;
            _parameters = parameters;
        }

        @Override
        public HttpUriRequestBase createRequest(ServerNode node) {
            String url = node.getUrl() + "/admin/databases/reorder?name=" + _databaseName;

            HttpPost request = new HttpPost(url);
            request.setEntity(new ContentProviderHttpEntity(outputStream -> {
                try (JsonGenerator generator = createSafeJsonGenerator(outputStream)) {
                    ObjectNode config = mapper.valueToTree(_parameters);
                    generator.writeTree(config);
                }
            }, ContentType.APPLICATION_JSON, _conventions));
            return request;
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
