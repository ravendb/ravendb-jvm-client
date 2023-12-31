package net.ravendb.client.serverwide.sharding;

import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.http.IRaftCommand;
import net.ravendb.client.http.RavenCommand;
import net.ravendb.client.http.ServerNode;
import net.ravendb.client.primitives.Reference;
import net.ravendb.client.serverwide.DatabaseTopology;
import net.ravendb.client.serverwide.operations.IServerOperation;
import net.ravendb.client.util.RaftIdGenerator;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;

import java.io.IOException;

public class AddDatabaseShardOperation implements IServerOperation<AddDatabaseShardOperation.AddDatabaseShardResult> {
    private final String _databaseName;
    private final Integer _shardNumber;
    private final String[] _nodes;
    private final Integer _replicationFactor;
    private final Boolean _dynamicNodeDistribution;

    public AddDatabaseShardOperation(String databaseName) {
        this(databaseName, null, false);
    }

    public AddDatabaseShardOperation(String databaseName, Integer shardNumber) {
        this(databaseName, shardNumber, false);
    }

    public AddDatabaseShardOperation(String databaseName, Integer shardNumber, boolean dynamicNodeDistribution) {
        this(databaseName, null, null, shardNumber, dynamicNodeDistribution);
    }

    public AddDatabaseShardOperation(String databaseName, Integer replicationFactor, Integer shardNumber, boolean dynamicNodeDistribution) {
        this(databaseName, null, replicationFactor, shardNumber, dynamicNodeDistribution);
    }

    public AddDatabaseShardOperation(String databaseName, String[] nodes, Integer replicationFactor, Integer shardNumber, boolean dynamicNodeDistribution) {
        _databaseName = databaseName;
        _shardNumber = shardNumber;
        _dynamicNodeDistribution = dynamicNodeDistribution;
        _replicationFactor = replicationFactor;
        _nodes = nodes;
    }

    @Override
    public RavenCommand<AddDatabaseShardResult> getCommand(DocumentConventions conventions) {
        return new AddDatabaseShardCommand(_databaseName, _shardNumber, _nodes, _replicationFactor, _dynamicNodeDistribution);
    }

    private static class AddDatabaseShardCommand extends RavenCommand<AddDatabaseShardResult> implements IRaftCommand {
        private final String _databaseName;
        private final Integer _shardNumber;
        private final String[] _nodes;
        private final Integer _replicationFactor;
        private final Boolean _dynamicNodeDistribution;

        public AddDatabaseShardCommand(String databaseName, Integer shardNumber, String[] nodes, Integer replicationFactor, Boolean dynamicNodeDistribution) {
            super(AddDatabaseShardResult.class);

            _databaseName = databaseName;
            _shardNumber = shardNumber;
            _nodes = nodes;
            _replicationFactor = replicationFactor;
            _dynamicNodeDistribution = dynamicNodeDistribution;
        }

        @Override
        public boolean isReadRequest() {
            return false;
        }

        @Override
        public HttpRequestBase createRequest(ServerNode node, Reference<String> url) {
            StringBuilder sb = new StringBuilder(node.getUrl())
                    .append("/admin/databases/shard?name=")
                    .append(urlEncode(_databaseName));

            if (_shardNumber != null) {
                sb.append("&shardNumber=")
                        .append(_shardNumber);
            }
            if (_replicationFactor != null) {
                sb.append("&replicationFactor=")
                        .append(_replicationFactor);
            }
            if (_dynamicNodeDistribution != null) {
                sb.append("&dynamicNodeDistribution=")
                        .append(_dynamicNodeDistribution);
            }

            if (_nodes != null && _nodes.length > 0) {
                for (String nodeStr : _nodes) {
                    sb.append("&node=")
                            .append(urlEncode(nodeStr));
                }
            }

            url.value = sb.toString();
            return new HttpPut();
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

    public static class AddDatabaseShardResult {
        private String name;
        private int shardNumber;
        private DatabaseTopology shardTopology;
        private long raftCommandIndex;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getShardNumber() {
            return shardNumber;
        }

        public void setShardNumber(int shardNumber) {
            this.shardNumber = shardNumber;
        }

        public DatabaseTopology getShardTopology() {
            return shardTopology;
        }

        public void setShardTopology(DatabaseTopology shardTopology) {
            this.shardTopology = shardTopology;
        }

        public long getRaftCommandIndex() {
            return raftCommandIndex;
        }

        public void setRaftCommandIndex(long raftCommandIndex) {
            this.raftCommandIndex = raftCommandIndex;
        }
    }
}
