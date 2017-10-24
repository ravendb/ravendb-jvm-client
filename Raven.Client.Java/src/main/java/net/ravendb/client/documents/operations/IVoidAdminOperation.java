package net.ravendb.client.documents.operations;

import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.http.RavenCommand;
import net.ravendb.client.http.VoidRavenCommand;

/**
 * Represents admin operation which doesn't return any response
 */
public interface IVoidAdminOperation extends IAdminOperation<Void> {
    VoidRavenCommand getCommand(DocumentConventions conventions);
}
