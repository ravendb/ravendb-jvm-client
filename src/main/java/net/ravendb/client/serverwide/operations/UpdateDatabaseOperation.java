package net.ravendb.client.serverwide.operations;

import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.http.RavenCommand;
import net.ravendb.client.serverwide.DatabaseRecord;
import net.ravendb.client.serverwide.DatabaseTopology;

public class UpdateDatabaseOperation implements IServerOperation<DatabasePutResult> {

    private final DatabaseRecord _databaseRecord;
    private final long _etag;
    private final int _replicationFactor;

    public UpdateDatabaseOperation(DatabaseRecord databaseRecord, long etag) {
        _databaseRecord = databaseRecord;
        _etag = etag;
        DatabaseTopology topology = databaseRecord.getTopology();
        if (topology != null && topology.getReplicationFactor() > 0) {
            _replicationFactor = topology.getReplicationFactor();
        } else {
            throw new IllegalArgumentException("DatabaseRecord.Topology.ReplicationFactor is missing");
        }
    }

    public UpdateDatabaseOperation(DatabaseRecord databaseRecord, int replicationFactor, long etag) {
        _databaseRecord = databaseRecord;
        _replicationFactor = replicationFactor;
        _etag = etag;
    }

    @Override
    public RavenCommand<DatabasePutResult> getCommand(DocumentConventions conventions) {
        return new CreateDatabaseOperation.CreateDatabaseCommand(conventions, _databaseRecord, _replicationFactor, _etag);
    }

}
