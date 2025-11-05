package net.ravendb.client.test.client.documents.AI;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.AI.TextPart;
import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.AI.AiStreamCallback;
import net.ravendb.client.documents.operations.AI.agents.ConversationResult;
import net.ravendb.client.documents.operations.AI.agents.RunConversationOperation;
import net.ravendb.client.http.RavenCommand;
import net.ravendb.client.infrastructure.EnableOnServer;
import org.junit.jupiter.api.Test;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import static org.assertj.core.api.Assertions.assertThat;

@EnableOnServer(thresholdVersion = "7.1")
public class AiStreamingTests<TAnswer> extends RemoteTestBase {

    @Test
    public void shouldParseStreamingResponseCorrectly() throws Exception {
        String streamingResponse =
                "\"Hello\"\n" +
                        "\"World\"\n" +
                        "\"!\"\n" +
                        "{\"conversationId\":\"conv/1-A\",\"response\":{\"message\":\"Hello World!\"},\"changeVector\":\"A:1-xyz\",\"actionRequests\":[]}";

        List<String> receivedChunks = new ArrayList<>();

        AiStreamCallback streamCallback = chunk -> {
            receivedChunks.add(chunk);
            return CompletableFuture.completedFuture(null);
        };

        RunConversationOperation<TAnswer> operation = new RunConversationOperation<>(
                "agents/1-A",
                "conv/1|",
                Collections.singletonList(new TextPart("Test prompt")),
                new ArrayList<>(),
                null,
                null,
                "message",
                streamCallback
        );

        DocumentConventions conventions = new DocumentConventions();
        RavenCommand<ConversationResult<TAnswer>> command = operation.getCommand(conventions);

        ByteArrayInputStream bodyStream = new ByteArrayInputStream(streamingResponse.getBytes(StandardCharsets.UTF_8));
        command.setResponseAsync(bodyStream, false).get();

        assertThat(receivedChunks).hasSize(3);
        assertThat(receivedChunks.get(0)).isEqualTo("Hello");
        assertThat(receivedChunks.get(1)).isEqualTo("World");
        assertThat(receivedChunks.get(2)).isEqualTo("!");

        ConversationResult<TAnswer> result = command.getResult();
        assertThat(result).isNotNull();
        assertThat(result.getConversationId()).isEqualTo("conv/1-A");
        assertThat(result.getResponse()).isNotNull();
        HashMap<String,String> responseMap = (HashMap<String,String>) result.getResponse();
        assertThat(responseMap.get("message")).isEqualTo("Hello World!");
    }

    @Test
    public void shouldHandleNonStreamingResponseCorrectly() throws Exception {
        String normalResponse = "{\"conversationId\":\"conv/2-A\",\"response\":{\"message\":\"Direct\"},\"changeVector\":\"A:2-xyz\",\"actionRequests\":[]}";

        List<String> receivedChunks = new ArrayList<>();

        AiStreamCallback streamCallback = chunk -> {
            receivedChunks.add(chunk);
            return CompletableFuture.completedFuture(null);
        };

        RunConversationOperation<HashMap<String, String>> operation = new RunConversationOperation<>(
                "agents/1-A",
                "conv/2|",
                Collections.singletonList(new TextPart("Test prompt")),
                new ArrayList<>(),
                null,
                null,
                "message",
                streamCallback
        );

        DocumentConventions conventions = new DocumentConventions();
        RavenCommand<ConversationResult<HashMap<String, String>>> command = operation.getCommand(conventions);

        ByteArrayInputStream bodyStream = new ByteArrayInputStream(normalResponse.getBytes(StandardCharsets.UTF_8));
        command.setResponseAsync(bodyStream, false).get();

        ConversationResult<HashMap<String, String>> result = command.getResult();
        assertThat(result).isNotNull();
        assertThat(result.getConversationId()).isEqualTo("conv/2-A");

        HashMap<String, String> responseMap = result.getResponse();
        assertThat(responseMap).isNotNull();
        assertThat(responseMap.get("message")).isEqualTo("Direct");
    }

    @Test
    public void shouldHandleEmptyLinesInStreamingResponse() throws Exception {
        String streamingResponse =
                "\"Chunk1\"\n" +
                        "\"Chunk2\"\n" +
                        "{\"conversationId\":\"conv/3-A\",\"response\":{\"text\":\"Done\"},\"changeVector\":\"A:3-xyz\",\"actionRequests\":[]}";

        List<String> receivedChunks = new ArrayList<>();

        AiStreamCallback streamCallback = chunk -> {
            receivedChunks.add(chunk);
            return CompletableFuture.completedFuture(null);
        };

        RunConversationOperation<HashMap<String, String>> operation = new RunConversationOperation<>(
                "agents/1-A",
                "conv/3|",
                Collections.singletonList(new TextPart("Test prompt")),
                new ArrayList<>(),
                null,
                null,
                "text",
                streamCallback
        );

        DocumentConventions conventions = new DocumentConventions();
        RavenCommand<ConversationResult<HashMap<String, String>>> command = operation.getCommand(conventions);

        ByteArrayInputStream bodyStream = new ByteArrayInputStream(streamingResponse.getBytes(StandardCharsets.UTF_8));
        command.setResponseAsync(bodyStream, false).get();

        assertThat(receivedChunks).hasSize(2);
        assertThat(receivedChunks.get(0)).isEqualTo("Chunk1");
        assertThat(receivedChunks.get(1)).isEqualTo("Chunk2");

        ConversationResult<HashMap<String, String>> result = command.getResult();
        assertThat(result).isNotNull();
        assertThat(result.getConversationId()).isEqualTo("conv/3-A");

        HashMap<String, String> responseMap = result.getResponse();
        assertThat(responseMap).isNotNull();
        assertThat(responseMap.get("text")).isEqualTo("Done");
    }
}
