package net.ravendb.client.documents.operations.backups;

import net.ravendb.client.primitives.UseSharpEnum;

@UseSharpEnum
public enum EncryptionMode {
    NONE,
    USE_DATABASE_KEY,
    USE_PROVIDED_KEY
}
