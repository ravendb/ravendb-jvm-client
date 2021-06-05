package net.ravendb.client.documents.indexes;

import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.conventions.DocumentConventions;

public interface IAbstractIndexCreationTask {
    String getIndexName();
    IndexPriority getPriority();
    IndexState getState();
    IndexDeploymentMode getDeploymentMode();
    DocumentConventions getConventions();
    void setConventions(DocumentConventions conventions);
    IndexDefinition createIndexDefinition();
    void execute(IDocumentStore store);
    void execute(IDocumentStore store, DocumentConventions conventions);
    void execute(IDocumentStore store, DocumentConventions conventions, String database);
}
