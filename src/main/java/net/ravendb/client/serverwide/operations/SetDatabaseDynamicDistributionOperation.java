package net.ravendb.client.serverwide.operations;

import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.http.IRaftCommand;
import net.ravendb.client.http.ServerNode;
import net.ravendb.client.http.VoidRavenCommand;
import net.ravendb.client.primitives.Reference;
import net.ravendb.client.util.RaftIdGenerator;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;

public class SetDatabaseDynamicDistributionOperation implements IVoidServerOperation {

    private final boolean _allowDynamicDistribution;
    private final String _databaseName;

    public SetDatabaseDynamicDistributionOperation(String databaseName, boolean allowDynamicDistribution) {
        if (StringUtils.isEmpty(databaseName)) {
            throw new IllegalArgumentException("DatabaseNAme should not be null or empty");
        }

        _allowDynamicDistribution = allowDynamicDistribution;
        _databaseName = databaseName;
    }

    @Override
    public VoidRavenCommand getCommand(DocumentConventions conventions) {
        return new SetDatabaseDynamicDistributionCommand(_databaseName, _allowDynamicDistribution);
    }

    private static class SetDatabaseDynamicDistributionCommand extends VoidRavenCommand implements IRaftCommand {
        private final String _databaseName;
        private final boolean _allowDynamicDistribution;

        public SetDatabaseDynamicDistributionCommand(String databaseName, boolean allowDynamicDistribution) {
            _databaseName = databaseName;
            _allowDynamicDistribution = allowDynamicDistribution;
        }

        @Override
        public boolean isReadRequest() {
            return false;
        }

        @Override
        public HttpRequestBase createRequest(ServerNode node, Reference<String> url) {
            url.value = node.getUrl() + "/admin/databases/dynamic-node-distribution?name=" + _databaseName + "&enabled=" + _allowDynamicDistribution;

            return new HttpPost();
        }

        @Override
        public String getRaftUniqueRequestId() {
            return RaftIdGenerator.newId();
        }
    }
}
