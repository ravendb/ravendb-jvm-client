package net.ravendb.client.serverwide.operations;

import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.http.VoidRavenCommand;

/**
 * Represents server operation which doesn't return any response
 */
public interface IVoidServerOperation extends IServerOperation<Void> {
    VoidRavenCommand getCommand(DocumentConventions conventions);
}
