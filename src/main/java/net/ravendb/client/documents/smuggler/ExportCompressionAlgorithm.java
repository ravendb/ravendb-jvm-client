package net.ravendb.client.documents.smuggler;

import net.ravendb.client.primitives.UseSharpEnum;

@UseSharpEnum
public enum ExportCompressionAlgorithm {
    ZSTD,
    GZIP
}
