package net.ravendb.client.serverwide.operations;

import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.http.RavenCommand;

public interface IMaintenanceOperation<T> {
    RavenCommand<T> getCommand(DocumentConventions conventions);
}
