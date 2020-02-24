package net.ravendb.client.documents.operations.backups;

import net.ravendb.client.primitives.UseSharpEnum;

@UseSharpEnum
public enum RestoreType {
    LOCAL,
    S3,
    AZURE,
    GOOGLE_CLOUD
}
