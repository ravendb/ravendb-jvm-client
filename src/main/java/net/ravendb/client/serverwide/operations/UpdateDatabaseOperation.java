package net.ravendb.client.serverwide.operations;

import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.http.RavenCommand;
import net.ravendb.client.serverwide.DatabaseRecord;

public class UpdateDatabaseOperation implements IServerOperation<DatabasePutResult> {

    private final DatabaseRecord _databaseRecord;
    private final long _etag;

    public UpdateDatabaseOperation(DatabaseRecord databaseRecord, long etag) {
        _databaseRecord = databaseRecord;
        _etag = etag;
    }

    @Override
    public RavenCommand<DatabasePutResult> getCommand(DocumentConventions conventions) {
        return new CreateDatabaseOperation.CreateDatabaseCommand(conventions, _databaseRecord, 1, _etag);
    }

}
