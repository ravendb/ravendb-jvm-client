package net.ravendb.client.test.client.documents.AI;

import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.operations.AI.AiUsage;
import net.ravendb.client.documents.operations.AI.agents.AiConversationDetailLevel;
import net.ravendb.client.documents.operations.AI.agents.AiConversationMessage;
import net.ravendb.client.documents.operations.AI.agents.AiConversationMessagesResult;
import net.ravendb.client.documents.operations.AI.agents.AiMessageRole;
import net.ravendb.client.documents.operations.AI.agents.AiToolCallResult;
import net.ravendb.client.documents.operations.AI.agents.GetConversationMessagesOperation;
import net.ravendb.client.documents.operations.AI.agents.GetConversationMessagesOptions;
import net.ravendb.client.extensions.JsonExtensions;
import net.ravendb.client.http.RavenCommand;
import net.ravendb.client.http.ServerNode;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.junit.jupiter.api.Test;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Covers the client-observable behaviour of {@link GetConversationMessagesOperation}: argument
 * validation, the request it builds, and deserialization of the response shape. Driving a real
 * conversation requires an LLM, which the Java test suite does not provision.
 */
public class GetConversationMessagesTest {

    private static Date utc(int year, int month, int day, int hour, int minute, int second) {
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        calendar.clear();
        calendar.set(year, month - 1, day, hour, minute, second);
        return calendar.getTime();
    }

    private static String buildUrl(GetConversationMessagesOptions options) throws Exception {
        RavenCommand<AiConversationMessagesResult> command =
                new GetConversationMessagesOperation(options).getCommand(new DocumentConventions());

        ServerNode node = new ServerNode();
        node.setUrl("http://localhost:8080");
        node.setDatabase("db1");

        HttpUriRequestBase request = command.createRequest(node);
        return request.getUri().toString();
    }

    @Test
    public void conversationIdIsRequired() {
        assertThatThrownBy(() -> new GetConversationMessagesOperation((String) null))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> new GetConversationMessagesOperation(""))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> new GetConversationMessagesOperation((GetConversationMessagesOptions) null))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> new GetConversationMessagesOperation(new GetConversationMessagesOptions()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void beforeAndAfterCannotBothBeSpecified() {
        GetConversationMessagesOptions options = new GetConversationMessagesOptions();
        options.setConversationId("conversations/1-A");
        options.setBefore(new Date());
        options.setAfter(new Date());

        assertThatThrownBy(() -> new GetConversationMessagesOperation(options))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot both be specified");
    }

    @Test
    public void pageSizeMustBePositive() {
        GetConversationMessagesOptions options = new GetConversationMessagesOptions();
        options.setConversationId("conversations/1-A");
        options.setPageSize(0);

        assertThatThrownBy(() -> new GetConversationMessagesOperation(options))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("PageSize");
    }

    @Test
    public void defaultsAreSimpleDetailLevelAndUnboundedPageSize() {
        GetConversationMessagesOptions options = new GetConversationMessagesOptions();
        assertThat(options.getDetailLevel()).isEqualTo(AiConversationDetailLevel.SIMPLE);
        assertThat(options.getPageSize()).isEqualTo(Integer.MAX_VALUE);
    }

    @Test
    public void buildsRequestWithConversationIdPageSizeAndDetailLevel() throws Exception {
        GetConversationMessagesOptions options = new GetConversationMessagesOptions();
        options.setConversationId("conversations/1-A");

        String url = buildUrl(options);

        assertThat(url).startsWith("http://localhost:8080/databases/db1/ai/agent/conversation/messages?");
        assertThat(url).contains("conversationId=conversations%2F1-A");
        assertThat(url).contains("&pageSize=" + Integer.MAX_VALUE);
        assertThat(url).contains("&detailLevel=Simple");
        assertThat(url).doesNotContain("before=");
        assertThat(url).doesNotContain("after=");
    }

    @Test
    public void detailLevelIsSentUsingTheServerName() throws Exception {
        GetConversationMessagesOptions options = new GetConversationMessagesOptions();
        options.setConversationId("conversations/1-A");
        options.setDetailLevel(AiConversationDetailLevel.DETAILED);
        assertThat(buildUrl(options)).contains("&detailLevel=Detailed");

        options.setDetailLevel(AiConversationDetailLevel.FULL);
        assertThat(buildUrl(options)).contains("&detailLevel=Full");
    }

    @Test
    public void beforeIsSentAsUtcIso8601() throws Exception {
        GetConversationMessagesOptions options = new GetConversationMessagesOptions();
        options.setConversationId("conversations/1-A");
        options.setBefore(utc(2026, 6, 16, 10, 30, 0));
        options.setPageSize(25);

        String url = buildUrl(options);

        // The .NET default Raven format carries 7 fractional-second digits.
        assertThat(url).contains("&before=2026-06-16T10%3A30%3A00.0000000Z");
        assertThat(url).contains("&pageSize=25");
        assertThat(url).doesNotContain("after=");
    }

    @Test
    public void afterIsSentAsUtcIso8601() throws Exception {
        GetConversationMessagesOptions options = new GetConversationMessagesOptions();
        options.setConversationId("conversations/1-A");
        options.setAfter(utc(2026, 6, 16, 10, 30, 0));

        String url = buildUrl(options);

        // The .NET default Raven format carries 7 fractional-second digits.
        assertThat(url).contains("&after=2026-06-16T10%3A30%3A00.0000000Z");
        assertThat(url).doesNotContain("before=");
    }

    @Test
    @SuppressWarnings("unchecked")
    public void canDeserializeConversationMessagesResult() throws Exception {
        String json = "{"
                + "\"ConversationId\":\"conversations/1-A\","
                + "\"Agent\":\"agents/1-A\","
                + "\"Parameters\":{\"name\":\"John\",\"age\":30,\"tags\":[\"a\",\"b\"],\"active\":true},"
                + "\"TotalUsage\":{\"PromptTokens\":10,\"CompletionTokens\":5,\"TotalTokens\":15,\"CachedTokens\":2,\"ReasoningTokens\":1},"
                + "\"LastMessageAt\":\"2026-06-16T10:30:00.0000000Z\","
                + "\"HasMoreMessages\":true,"
                + "\"SubConversationIds\":[\"conversations/2-A\"],"
                + "\"Attachments\":[\"file.txt\"],"
                + "\"Messages\":["
                + "  {\"Role\":\"User\",\"Content\":\"hello\",\"Timestamp\":\"2026-06-16T10:29:00.0000000Z\"},"
                + "  {\"Role\":\"Assistant\",\"Content\":null,\"Timestamp\":\"2026-06-16T10:30:00.0000000Z\","
                + "   \"Usage\":{\"PromptTokens\":10,\"CompletionTokens\":5,\"TotalTokens\":15,\"CachedTokens\":0,\"ReasoningTokens\":0},"
                + "   \"ToolCalls\":[{\"Id\":\"call_1\",\"Name\":\"lookup\",\"Arguments\":\"{}\",\"Result\":\"ok\",\"SubConversationId\":\"conversations/2-A\"}]}"
                + "]}";

        AiConversationMessagesResult result =
                JsonExtensions.getDefaultMapper().readValue(json, AiConversationMessagesResult.class);

        assertThat(result.getConversationId()).isEqualTo("conversations/1-A");
        assertThat(result.getAgent()).isEqualTo("agents/1-A");
        assertThat(result.isHasMoreMessages()).isTrue();
        assertThat(result.getSubConversationIds()).containsExactly("conversations/2-A");
        assertThat(result.getAttachments()).containsExactly("file.txt");
        assertThat(result.getLastMessageAt()).isEqualTo(utc(2026, 6, 16, 10, 30, 0));

        assertThat(result.getParameters())
                .containsEntry("name", "John")
                .containsEntry("age", 30)
                .containsEntry("active", true);
        List<Object> tags = (List<Object>) result.getParameters().get("tags");
        assertThat(tags).containsExactly("a", "b");

        AiUsage totalUsage = result.getTotalUsage();
        assertThat(totalUsage.getTotalTokens()).isEqualTo(15);
        assertThat(totalUsage.getReasoningTokens()).isEqualTo(1);

        assertThat(result.getMessages()).hasSize(2);

        AiConversationMessage user = result.getMessages().get(0);
        assertThat(user.getRole()).isEqualTo(AiMessageRole.USER);
        assertThat(user.getContent()).isEqualTo("hello");
        assertThat(user.getTimestamp()).isEqualTo(utc(2026, 6, 16, 10, 29, 0));

        AiConversationMessage assistant = result.getMessages().get(1);
        assertThat(assistant.getRole()).isEqualTo(AiMessageRole.ASSISTANT);
        assertThat(assistant.getContent()).isNull();
        assertThat(assistant.getUsage().getCompletionTokens()).isEqualTo(5);
        assertThat(assistant.getToolCalls()).hasSize(1);

        AiToolCallResult toolCall = assistant.getToolCalls().get(0);
        assertThat(toolCall.getId()).isEqualTo("call_1");
        assertThat(toolCall.getName()).isEqualTo("lookup");
        assertThat(toolCall.getResult()).isEqualTo("ok");
        assertThat(toolCall.getSubConversationId()).isEqualTo("conversations/2-A");
    }

    @Test
    public void canDeserializeAllMessageRoles() throws Exception {
        String json = "{\"Messages\":["
                + "{\"Role\":\"System\"},{\"Role\":\"User\"},{\"Role\":\"Assistant\"},"
                + "{\"Role\":\"Summary\"},{\"Role\":\"Internal\",\"SubConversationId\":\"conversations/3-A\"}"
                + "]}";

        AiConversationMessagesResult result =
                JsonExtensions.getDefaultMapper().readValue(json, AiConversationMessagesResult.class);

        assertThat(result.getMessages())
                .extracting(AiConversationMessage::getRole)
                .containsExactly(AiMessageRole.SYSTEM, AiMessageRole.USER, AiMessageRole.ASSISTANT,
                        AiMessageRole.SUMMARY, AiMessageRole.INTERNAL);

        assertThat(result.getMessages().get(4).getSubConversationId()).isEqualTo("conversations/3-A");
    }
}
