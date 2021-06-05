package net.ravendb.client.documents.operations;

import net.ravendb.client.util.Size;

public class CollectionDetails {
    private String name;
    private long countOfDocuments;
    private Size size;
    private Size documentsSize;
    private Size tombstonesSize;
    private Size revisionsSize;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getCountOfDocuments() {
        return countOfDocuments;
    }

    public void setCountOfDocuments(long countOfDocuments) {
        this.countOfDocuments = countOfDocuments;
    }

    public Size getSize() {
        return size;
    }

    public void setSize(Size size) {
        this.size = size;
    }

    public Size getDocumentsSize() {
        return documentsSize;
    }

    public void setDocumentsSize(Size documentsSize) {
        this.documentsSize = documentsSize;
    }

    public Size getTombstonesSize() {
        return tombstonesSize;
    }

    public void setTombstonesSize(Size tombstonesSize) {
        this.tombstonesSize = tombstonesSize;
    }

    public Size getRevisionsSize() {
        return revisionsSize;
    }

    public void setRevisionsSize(Size revisionsSize) {
        this.revisionsSize = revisionsSize;
    }
}
