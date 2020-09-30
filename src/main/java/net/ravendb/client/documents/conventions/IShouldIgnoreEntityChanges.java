package net.ravendb.client.documents.conventions;

import net.ravendb.client.documents.session.InMemoryDocumentSessionOperations;

@FunctionalInterface
public interface IShouldIgnoreEntityChanges {
    boolean check(InMemoryDocumentSessionOperations sessionOperations, Object entity, String documentId);
}
