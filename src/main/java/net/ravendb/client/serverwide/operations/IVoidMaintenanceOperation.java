package net.ravendb.client.serverwide.operations;

import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.operations.IMaintenanceOperation;
import net.ravendb.client.http.VoidRavenCommand;


/**
 * Represents server operation which doesn't return any response
 */
public interface IVoidMaintenanceOperation extends IMaintenanceOperation<Void> {
    VoidRavenCommand getCommand(DocumentConventions conventions);
}
