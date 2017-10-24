package net.ravendb.client.documents.operations;

import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.http.HttpCache;
import net.ravendb.client.http.RavenCommand;

public interface IOperation<T> {
    RavenCommand<T> getCommand(IDocumentStore store, DocumentConventions conventions, HttpCache cache);
}
