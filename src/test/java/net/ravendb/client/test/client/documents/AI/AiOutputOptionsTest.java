package net.ravendb.client.test.client.documents.AI;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.ravendb.client.documents.AI.AiConversationCreationOptions;
import net.ravendb.client.documents.AI.AiOutputOptions;
import net.ravendb.client.documents.AI.ContentPart;
import net.ravendb.client.documents.AI.TextPart;
import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.operations.AI.agents.RunConversationOperation;
import net.ravendb.client.extensions.JsonExtensions;
import net.ravendb.client.http.ServerNode;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class AiOutputOptionsTest {

    @Test
    public void constructorsValidateArguments() {
        assertThatThrownBy(() -> new AiOutputOptions((Object) null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new AiOutputOptions((String) null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new AiOutputOptions("   "))
                .isInstanceOf(IllegalArgumentException.class);
        // a String reaching the sample-object constructor (e.g. via generic overloads) is rejected, like C#
        assertThatThrownBy(() -> new AiOutputOptions((Object) "{\"type\":\"object\"}"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("raw string");
    }

    @Test
    public void sampleObjectIsSentAsJsonString() throws Exception {
        JsonNode outputOptions = requestBody(new AiOutputOptions(new AlternativeSchema("a short summary", 5)))
                .get("OutputOptions");

        assertThat(outputOptions.get("SampleObject").isTextual()).isTrue();
        JsonNode sampleObject = JsonExtensions.getDefaultMapper().readTree(outputOptions.get("SampleObject").asText());
        assertThat(sampleObject.get("summary").asText()).isEqualTo("a short summary");
        assertThat(sampleObject.get("score").asInt()).isEqualTo(5);

        assertThat(outputOptions.has("OutputSchema")).isFalse();
        assertThat(outputOptions.has("NoSchema")).isFalse();
    }

    @Test
    public void outputSchemaIsSent() throws Exception {
        String schema = "{\"name\":\"alt_output\",\"strict\":true}";

        JsonNode outputOptions = requestBody(new AiOutputOptions(schema)).get("OutputOptions");

        assertThat(outputOptions.get("OutputSchema").asText()).isEqualTo(schema);
        assertThat(outputOptions.has("SampleObject")).isFalse();
        assertThat(outputOptions.has("NoSchema")).isFalse();
    }

    @Test
    public void noSchemaIsSent() throws Exception {
        AiOutputOptions options = new AiOutputOptions();
        options.setNoSchema(true);

        JsonNode outputOptions = requestBody(options).get("OutputOptions");

        assertThat(outputOptions.get("NoSchema").asBoolean()).isTrue();
        assertThat(outputOptions.size()).isEqualTo(1);
    }

    @Test
    public void outputOptionsAreOmittedWhenNotSet() throws Exception {
        assertThat(requestBody(null).has("OutputOptions")).isFalse();
    }

    private static JsonNode requestBody(AiOutputOptions outputOptions) throws Exception {
        DocumentConventions conventions = new DocumentConventions();
        conventions.setUseHttpCompression(false);

        RunConversationOperation<Object> operation = new RunConversationOperation<>("agents/1-A", "chats/1",
                Collections.<ContentPart>singletonList(new TextPart("hello")), Collections.emptyList(), Collections.emptyList(),
                new AiConversationCreationOptions(), null, null, null, null, outputOptions);

        ServerNode node = new ServerNode();
        node.setUrl("http://127.0.0.1:8080");
        node.setDatabase("db1");

        HttpUriRequestBase request = operation.getCommand(conventions).createRequest(node);

        ByteArrayOutputStream body = new ByteArrayOutputStream();
        request.getEntity().writeTo(body);

        ObjectMapper mapper = JsonExtensions.getDefaultMapper();
        return mapper.readTree(body.toByteArray());
    }

    public static class AlternativeSchema {
        private String summary;
        private int score;

        public AlternativeSchema() {
        }

        public AlternativeSchema(String summary, int score) {
            this.summary = summary;
            this.score = score;
        }

        public String getSummary() {
            return summary;
        }

        public void setSummary(String summary) {
            this.summary = summary;
        }

        public int getScore() {
            return score;
        }

        public void setScore(int score) {
            this.score = score;
        }
    }
}
