package net.ravendb.client.documents.operations.backups;

import net.ravendb.client.primitives.UseSharpEnum;

@UseSharpEnum
public enum UploadState {
    PENDING_UPLOAD,
    UPLOADING,
    PENDING_RESPONSE,
    DONE
}
