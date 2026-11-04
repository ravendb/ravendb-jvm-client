package net.ravendb.client.test.issues;

import net.ravendb.client.documents.AI.AiConversationCreationOptions;
import net.ravendb.client.documents.AI.ContentPart;
import net.ravendb.client.documents.AI.TextPart;
import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.operations.AI.agents.ConversationResult;
import net.ravendb.client.documents.operations.AI.agents.RunConversationOperation;
import net.ravendb.client.exceptions.AiException;
import net.ravendb.client.exceptions.RefusedToAnswerException;
import net.ravendb.client.http.RavenCommand;
import org.apache.hc.core5.http.message.BasicClassicHttpResponse;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The C# test provokes the failure through a server test hook, which the Java client cannot reach,
 * so this verifies the client side of the contract directly: an exception the server writes into an
 * already started (HTTP 200) stream must be thrown, not parsed as the conversation result.
 */
public class RavenDB_25401Test {

    private final List<String> chunks = new ArrayList<>();

    private RavenCommand<ConversationResult<Object>> createStreamingCommand() {
        List<ContentPart> prompt = Collections.singletonList(new TextPart("hello"));
        RunConversationOperation<Object> operation = new RunConversationOperation<>("agent", "chats/", prompt,
                Collections.emptyList(), Collections.emptyList(), new AiConversationCreationOptions(), null,
                "Answer", chunk -> {
                    chunks.add(chunk);
                    return CompletableFuture.completedFuture(null);
                });

        return operation.getCommand(new DocumentConventions());
    }

    private static ByteArrayInputStream stream(String... lines) {
        return new ByteArrayInputStream(String.join("\n", lines).getBytes(StandardCharsets.UTF_8));
    }

    @Test
    public void errorWrittenAfterStreamingStartedIsThrown() {
        RavenCommand<ConversationResult<Object>> command = createStreamingCommand();

        String error = "{\"Url\":\"/databases/db/ai/agent\",\"Type\":\"Raven.Client.Exceptions.AiException\"," +
                "\"Message\":\"RefusedToAnswerException: I'm sorry, I can't help with that.\"," +
                "\"Error\":\"Raven.Client.Exceptions.RefusedToAnswerException: I'm sorry, I can't help with that.\"}";

        assertThatThrownBy(() -> command.setResponseRaw(new BasicClassicHttpResponse(200), stream("\"partial \"", error)))
                .isExactlyInstanceOf(AiException.class)
                .hasMessageContaining("RefusedToAnswerException")
                .hasMessageContaining("I'm sorry, I can't help with that.");

        assertThat(chunks).containsExactly("partial ");
        assertThat(command.getResult()).isNull();
    }

    @Test
    public void exceptionSpecificFieldsAreFilledFromTheStreamedError() {
        RavenCommand<ConversationResult<Object>> command = createStreamingCommand();

        String error = "{\"Type\":\"Raven.Client.Exceptions.RefusedToAnswerException\",\"Message\":\"refused\"," +
                "\"Error\":\"refused\",\"Refusal\":\"I can't\",\"FinishReason\":\"content_filter\"}";

        assertThatThrownBy(() -> command.setResponseRaw(new BasicClassicHttpResponse(200), stream(error)))
                .isInstanceOfSatisfying(RefusedToAnswerException.class, e -> {
                    assertThat(e.refusal).isEqualTo("I can't");
                    assertThat(e.finishReason).isEqualTo("content_filter");
                });
    }

    @Test
    public void finalResultIsStillParsed() throws Exception {
        RavenCommand<ConversationResult<Object>> command = createStreamingCommand();

        command.setResponseRaw(new BasicClassicHttpResponse(200),
                stream("\"Hel\"", "\"lo\"", "{\"ConversationId\":\"chats/1\",\"ChangeVector\":\"A:1-x\"}"));

        assertThat(chunks).containsExactly("Hel", "lo");
        assertThat(command.getResult()).isNotNull();
        assertThat(command.getResult().getConversationId()).isEqualTo("chats/1");
    }
}
