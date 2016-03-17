package net.ravendb.abstractions.smuggler;

import net.ravendb.abstractions.data.EnumSet;

public class ExportOptions {

    private boolean exportDocuments;
    private boolean exportAttachments;
    private boolean exportDeletions;
    private LastEtagsInfo startEtags;
    private int maxNumberOfDocumentsToExport;
    private int maxNumberOfAttachmentsToExport;


    private ExportOptions() {
        startEtags = new LastEtagsInfo();
        startEtags.setLastDocsEtag(null);
        startEtags.setLastDocDeleteEtag(null);
        startEtags.setLastAttachmentEtag(null);
        startEtags.setLastAttachmentDeleteEtag(null);

        maxNumberOfDocumentsToExport = Integer.MAX_VALUE;
        maxNumberOfAttachmentsToExport = Integer.MAX_VALUE;
    }


    public boolean isExportDocuments() {
        return exportDocuments;
    }

    public void setExportDocuments(boolean exportDocuments) {
        this.exportDocuments = exportDocuments;
    }

    public boolean isExportAttachments() {
        return exportAttachments;
    }

    public void setExportAttachments(boolean exportAttachments) {
        this.exportAttachments = exportAttachments;
    }

    public boolean isExportDeletions() {
        return exportDeletions;
    }

    public void setExportDeletions(boolean exportDeletions) {
        this.exportDeletions = exportDeletions;
    }

    public LastEtagsInfo getStartEtags() {
        return startEtags;
    }

    public void setStartEtags(LastEtagsInfo startEtags) {
        this.startEtags = startEtags;
    }

    public int getMaxNumberOfDocumentsToExport() {
        return maxNumberOfDocumentsToExport;
    }

    public void setMaxNumberOfDocumentsToExport(int maxNumberOfDocumentsToExport) {
        if (maxNumberOfDocumentsToExport < 0) {
            maxNumberOfDocumentsToExport = 0;
        }
        this.maxNumberOfDocumentsToExport = maxNumberOfDocumentsToExport;
    }

    public int getMaxNumberOfAttachmentsToExport() {
        return maxNumberOfAttachmentsToExport;
    }

    public void setMaxNumberOfAttachmentsToExport(int maxNumberOfAttachmentsToExport) {
        if (maxNumberOfAttachmentsToExport < 0) {
            maxNumberOfAttachmentsToExport = 0;
        }
        this.maxNumberOfAttachmentsToExport = maxNumberOfAttachmentsToExport;
    }

    public static ExportOptions create(OperationState state, ItemTypeSet types, boolean exportDeletions, int maxNumberOfItemsToExport) {
        ExportOptions options = new ExportOptions();
        options.setExportAttachments(types.contains(ItemType.ATTACHMENTS));
        options.setExportDocuments(types.contains(ItemType.DOCUMENTS));
        options.setExportDeletions(exportDeletions);
        options.setStartEtags(state);
        options.setMaxNumberOfDocumentsToExport(maxNumberOfItemsToExport - state.getNumberOfExportedDocuments());
        options.setMaxNumberOfAttachmentsToExport(maxNumberOfItemsToExport - state.getNumberOfExportedAttachments());

        return options;
    }
}
