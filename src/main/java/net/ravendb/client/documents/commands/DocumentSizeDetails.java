package net.ravendb.client.documents.commands;

public class DocumentSizeDetails {

    private String docId;
    private int actualSize;
    private String humaneActualSize;
    private int allocatedSize;
    private String humaneAllocatedSize;

    public String getDocId() {
        return docId;
    }

    public void setDocId(String docId) {
        this.docId = docId;
    }

    public int getActualSize() {
        return actualSize;
    }

    public void setActualSize(int actualSize) {
        this.actualSize = actualSize;
    }

    public String getHumaneActualSize() {
        return humaneActualSize;
    }

    public void setHumaneActualSize(String humaneActualSize) {
        this.humaneActualSize = humaneActualSize;
    }

    public int getAllocatedSize() {
        return allocatedSize;
    }

    public void setAllocatedSize(int allocatedSize) {
        this.allocatedSize = allocatedSize;
    }

    public String getHumaneAllocatedSize() {
        return humaneAllocatedSize;
    }

    public void setHumaneAllocatedSize(String humaneAllocatedSize) {
        this.humaneAllocatedSize = humaneAllocatedSize;
    }
}
