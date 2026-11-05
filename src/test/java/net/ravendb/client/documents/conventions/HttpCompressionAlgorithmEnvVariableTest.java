package net.ravendb.client.documents.conventions;

import net.ravendb.client.http.HttpCompressionAlgorithm;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class HttpCompressionAlgorithmEnvVariableTest {

    @Test
    public void missingValueReturnsDefault() {
        assertThat(DocumentConventions.parseHttpCompressionAlgorithm(null, HttpCompressionAlgorithm.Zstd))
                .isEqualTo(HttpCompressionAlgorithm.Zstd);
        assertThat(DocumentConventions.parseHttpCompressionAlgorithm(null, HttpCompressionAlgorithm.Gzip))
                .isEqualTo(HttpCompressionAlgorithm.Gzip);
    }

    @Test
    public void parsesCaseInsensitively() {
        assertThat(DocumentConventions.parseHttpCompressionAlgorithm("gzip", HttpCompressionAlgorithm.Zstd))
                .isEqualTo(HttpCompressionAlgorithm.Gzip);
        assertThat(DocumentConventions.parseHttpCompressionAlgorithm("GZIP", HttpCompressionAlgorithm.Zstd))
                .isEqualTo(HttpCompressionAlgorithm.Gzip);
        assertThat(DocumentConventions.parseHttpCompressionAlgorithm("Zstd", HttpCompressionAlgorithm.Gzip))
                .isEqualTo(HttpCompressionAlgorithm.Zstd);
        assertThat(DocumentConventions.parseHttpCompressionAlgorithm(" zstd ", HttpCompressionAlgorithm.Gzip))
                .isEqualTo(HttpCompressionAlgorithm.Zstd);
    }

    @Test
    public void invalidValueThrows() {
        assertThatThrownBy(() -> DocumentConventions.parseHttpCompressionAlgorithm("brotli", HttpCompressionAlgorithm.Zstd))
                .isExactlyInstanceOf(IllegalStateException.class)
                .hasMessage("Could not parse 'RAVEN_HTTP_COMPRESSION_ALGORITHM' env variable with value 'brotli'.");
    }

    @Test
    public void newConventionsUseDefaultAlgorithm() {
        assertThat(new DocumentConventions().getHttpCompressionAlgorithm())
                .isEqualTo(DocumentConventions.DEFAULT_HTTP_COMPRESSION_ALGORITHM);
    }
}
