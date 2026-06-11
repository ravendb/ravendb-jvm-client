package net.ravendb.client.documents.operations.AI;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.ravendb.client.extensions.JsonExtensions;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ChunkingOptionsTest {

    @Test
    public void contextPrefixSerializesWithSharpName() throws Exception {
        ObjectMapper mapper = JsonExtensions.getDefaultMapper();

        ChunkingOptions options = new ChunkingOptions(ChunkingMethod.PLAIN_TEXT_SPLIT_PARAGRAPHS);
        options.setContextPrefix("Document title");

        String json = mapper.writeValueAsString(options);

        assertThat(json).contains("\"ContextPrefix\":\"Document title\"");
    }

    @Test
    public void contextPrefixRoundTrips() throws Exception {
        ObjectMapper mapper = JsonExtensions.getDefaultMapper();

        ChunkingOptions options = new ChunkingOptions();
        options.setContextPrefix("prefix");

        ChunkingOptions deserialized = mapper.readValue(mapper.writeValueAsString(options), ChunkingOptions.class);

        assertThat(deserialized.getContextPrefix()).isEqualTo("prefix");
    }
}
