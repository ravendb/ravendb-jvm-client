package net.ravendb.client.http;

import net.ravendb.client.Constants;

public enum HttpCompressionAlgorithm {
    Gzip,
    Zstd;


    public static String getContentEncoding(HttpCompressionAlgorithm compressionAlgorithm) {
        switch (compressionAlgorithm) {
            case Gzip:
                return Constants.Headers.Encodings.GZIP;
            case Zstd:
                return Constants.Headers.Encodings.ZSTD;
            default:
                throw new IllegalArgumentException("Invalid compression algorithm: " + compressionAlgorithm);
        }
    }
}
