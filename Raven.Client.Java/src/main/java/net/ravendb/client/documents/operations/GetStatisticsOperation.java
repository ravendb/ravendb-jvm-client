package net.ravendb.client.documents.operations;

import net.ravendb.client.documents.commands.GetStatisticsCommand;
import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.http.RavenCommand;

public class GetStatisticsOperation implements IMaintenanceOperation<DatabaseStatistics> {

    @Override
    public RavenCommand<DatabaseStatistics> getCommand(DocumentConventions conventions) {
        return new GetStatisticsCommand();
    }
}
