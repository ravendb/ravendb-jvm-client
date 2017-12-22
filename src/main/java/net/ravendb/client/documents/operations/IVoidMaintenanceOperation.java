package net.ravendb.client.documents.operations;

import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.http.VoidRavenCommand;

/**
 * Represents admin operation which doesn't return any response
 */
public interface IVoidMaintenanceOperation extends IMaintenanceOperation<Void> {
    VoidRavenCommand getCommand(DocumentConventions conventions);
}
