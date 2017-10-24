package net.ravendb.client.documents.operations;

import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.http.HttpCache;
import net.ravendb.client.http.VoidRavenCommand;

/**
 * Represents operation which doesn't return any response
 */
public interface IVoidOperation extends IOperation<Void> {
    VoidRavenCommand getCommand(IDocumentStore store, DocumentConventions conventions, HttpCache cache);
}
