package net.ravendb.client.documents.operations.AI.agents;

import net.ravendb.client.documents.operations.AI.AiUsage;

import java.util.Date;
import java.util.List;

public class AiConversationMessage {

    private AiMessageRole role;
    private String content;
    private List<String> attachments;
    private Date timestamp;
    private List<AiToolCallResult> toolCalls;
    private AiUsage usage;
    private String subConversationId;

    /**
     * @return the role of the message sender
     */
    public AiMessageRole getRole() {
        return role;
    }

    public void setRole(AiMessageRole role) {
        this.role = role;
    }

    /**
     * @return the text content. When the stored message has multiple text parts, they are joined with
     *         line breaks. Null for assistant messages that only initiated tool calls.
     */
    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    /**
     * @return the attachment file names associated with this message, if any
     */
    public List<String> getAttachments() {
        return attachments;
    }

    public void setAttachments(List<String> attachments) {
        this.attachments = attachments;
    }

    /**
     * @return when this message was recorded (UTC). Guaranteed unique and monotonic within a
     *         conversation — safe to use as a paging cursor.
     */
    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    /**
     * @return the tool calls initiated by this assistant message, with their responses inlined.
     *         Empty when no tool calls are present (including for non-assistant messages).
     */
    public List<AiToolCallResult> getToolCalls() {
        return toolCalls;
    }

    public void setToolCalls(List<AiToolCallResult> toolCalls) {
        this.toolCalls = toolCalls;
    }

    /**
     * @return the token usage for this message (typically on assistant messages)
     */
    public AiUsage getUsage() {
        return usage;
    }

    public void setUsage(AiUsage usage) {
        this.usage = usage;
    }

    /**
     * @return for {@link AiMessageRole#INTERNAL} messages, the ID of the sub-conversation this message relates to
     */
    public String getSubConversationId() {
        return subConversationId;
    }

    public void setSubConversationId(String subConversationId) {
        this.subConversationId = subConversationId;
    }
}
