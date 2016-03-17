package net.ravendb.abstractions.smuggler;

public class OperationState extends LastEtagsInfo {
    private String filePath;
    private int numberOfExportedDocuments;
    private int numberOfExportedAttachments;

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public int getNumberOfExportedDocuments() {
        return numberOfExportedDocuments;
    }

    public void setNumberOfExportedDocuments(int numberOfExportedDocuments) {
        this.numberOfExportedDocuments = numberOfExportedDocuments;
    }

    public int getNumberOfExportedAttachments() {
        return numberOfExportedAttachments;
    }

    public void setNumberOfExportedAttachments(int numberOfExportedAttachments) {
        this.numberOfExportedAttachments = numberOfExportedAttachments;
    }
}
