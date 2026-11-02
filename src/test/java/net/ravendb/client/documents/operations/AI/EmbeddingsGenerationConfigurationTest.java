package net.ravendb.client.documents.operations.AI;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import net.ravendb.client.extensions.JsonExtensions;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class EmbeddingsGenerationConfigurationTest {

    @Test
    public void storeChunkTextSerializesWithSharpName() {
        ObjectMapper mapper = JsonExtensions.getDefaultMapper();

        EmbeddingsGenerationConfiguration configuration = new EmbeddingsGenerationConfiguration();
        configuration.setCollection("Dtos");

        ObjectNode json = mapper.valueToTree(configuration);
        assertThat(json.get("StoreChunkText").asBoolean()).isFalse();

        configuration.setStoreChunkText(true);

        json = mapper.valueToTree(configuration);
        assertThat(json.get("StoreChunkText").asBoolean()).isTrue();
    }

    @Test
    public void storeChunkTextDeserializes() throws Exception {
        EmbeddingsGenerationConfiguration configuration = JsonExtensions.getDefaultMapper()
                .readValue("{\"StoreChunkText\":true}", EmbeddingsGenerationConfiguration.class);

        assertThat(configuration.isStoreChunkText()).isTrue();
    }
}
