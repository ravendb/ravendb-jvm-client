package net.ravendb.client.util;

public class Size {
    private long sizeInBytes;
    private String humaneSize;

    public long getSizeInBytes() {
        return sizeInBytes;
    }

    public void setSizeInBytes(long sizeInBytes) {
        this.sizeInBytes = sizeInBytes;
    }

    public String getHumaneSize() {
        return humaneSize;
    }

    public void setHumaneSize(String humaneSize) {
        this.humaneSize = humaneSize;
    }
}
