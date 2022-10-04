package net.ravendb.client.documents.operations.etl.queue;

public class EtlQueue {

    private String name;
    private boolean deleteProcessedDocuments;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isDeleteProcessedDocuments() {
        return deleteProcessedDocuments;
    }

    public void setDeleteProcessedDocuments(boolean deleteProcessedDocuments) {
        this.deleteProcessedDocuments = deleteProcessedDocuments;
    }
}
