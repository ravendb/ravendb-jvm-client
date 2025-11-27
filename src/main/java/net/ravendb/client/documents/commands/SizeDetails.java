package net.ravendb.client.documents.commands;

public class SizeDetails {

    private int actualSize;
    private String humaneActualSize;
    private int allocatedSize;
    private String humaneAllocatedSize;
    private boolean isCompressed;

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

    public boolean isCompressed() {
        return isCompressed;
    }

    public void setCompressed(boolean isCompressed) {
        this.isCompressed = isCompressed;
    }
}

