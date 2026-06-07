package net.ravendb.client.test.client.documents.AI;

import com.fasterxml.jackson.databind.node.ObjectNode;
import net.ravendb.client.documents.AI.AiConversationCreationOptions;
import net.ravendb.client.documents.AI.AiConversationParameterOptions;
import net.ravendb.client.extensions.JsonExtensions;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class AiConversationOptionsSerializationTest {

    @Test
    public void parametersSerializeWithValueAndSendToModel() {
        AiConversationCreationOptions options = new AiConversationCreationOptions()
                .addParameter("company", "companies/90-A")
                .addParameter("ssn", "123-45-6789", new AiConversationParameterOptions(false));
        options.setMaxModelIterationsPerCall(7);
        options.setExpirationInSec(60);

        ObjectNode json = JsonExtensions.getDefaultMapper().valueToTree(options);

        ObjectNode parameters = (ObjectNode) json.get("Parameters");
        assertThat(parameters).isNotNull();

        ObjectNode company = (ObjectNode) parameters.get("company");
        assertThat(company.get("Value").asText()).isEqualTo("companies/90-A");
        assertThat(company.get("SendToModel").asBoolean()).isTrue();

        ObjectNode ssn = (ObjectNode) parameters.get("ssn");
        assertThat(ssn.get("Value").asText()).isEqualTo("123-45-6789");
        assertThat(ssn.get("SendToModel").asBoolean()).isFalse();

        assertThat(json.get("MaxModelIterationsPerCall").asInt()).isEqualTo(7);
        assertThat(json.get("ExpirationInSec").asInt()).isEqualTo(60);
    }
}
