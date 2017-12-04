package net.ravendb.client.documents.operations;

import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.http.RavenCommand;

public interface IMaintenanceOperation<TResult> {
    RavenCommand<TResult> getCommand(DocumentConventions conventions);
}
