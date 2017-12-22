package net.ravendb.client.documents.operations;

import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.http.RavenCommand;

import java.util.Map;

public class GetIdentitiesOperation implements IMaintenanceOperation<Map<String, Long>> {

    @Override
    public RavenCommand<Map<String, Long>> getCommand(DocumentConventions conventions) {
        return new GetIdentitiesCommand();
    }
}
