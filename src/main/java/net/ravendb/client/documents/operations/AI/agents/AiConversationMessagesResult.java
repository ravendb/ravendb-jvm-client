package net.ravendb.client.documents.operations.AI.agents;

import net.ravendb.client.documents.operations.AI.AiUsage;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * The result of fetching conversation messages.
 */
public class AiConversationMessagesResult {

    private String conversationId;
    private String agent;
    private Map<String, Object> parameters;
    private AiUsage totalUsage;
    private Date lastMessageAt;
    private List<AiConversationMessage> messages;
    private boolean hasMoreMessages;
    private List<String> subConversationIds;
    private List<String> attachments;

    /**
     * @return the conversation document ID
     */
    public String getConversationId() {
        return conversationId;
    }

    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }

    /**
     * @return the identifier of the AI agent this conversation belongs to
     */
    public String getAgent() {
        return agent;
    }

    public void setAgent(String agent) {
        this.agent = agent;
    }

    /**
     * @return the conversation parameters as a name to value map, normalized from the stored format.
     *         Values can be heterogeneous — strings, numbers, booleans, or lists.
     */
    public Map<String, Object> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, Object> parameters) {
        this.parameters = parameters;
    }

    /**
     * @return the cumulative token usage across all turns of this conversation
     */
    public AiUsage getTotalUsage() {
        return totalUsage;
    }

    public void setTotalUsage(AiUsage totalUsage) {
        this.totalUsage = totalUsage;
    }

    /**
     * @return when the last message was added to the conversation
     */
    public Date getLastMessageAt() {
        return lastMessageAt;
    }

    public void setLastMessageAt(Date lastMessageAt) {
        this.lastMessageAt = lastMessageAt;
    }

    /**
     * @return the messages in chronological order (oldest first)
     */
    public List<AiConversationMessage> getMessages() {
        return messages;
    }

    public void setMessages(List<AiConversationMessage> messages) {
        this.messages = messages;
    }

    /**
     * @return true if there are more messages beyond the returned page. For backward/default paging,
     *         older messages exist; for forward (after) paging, newer messages exist.
     */
    public boolean isHasMoreMessages() {
        return hasMoreMessages;
    }

    public void setHasMoreMessages(boolean hasMoreMessages) {
        this.hasMoreMessages = hasMoreMessages;
    }

    /**
     * @return the IDs of sub-agent conversations spawned during this conversation. Each can be queried
     *         separately via getConversationMessages.
     */
    public List<String> getSubConversationIds() {
        return subConversationIds;
    }

    public void setSubConversationIds(List<String> subConversationIds) {
        this.subConversationIds = subConversationIds;
    }

    /**
     * @return all attachments referenced across the conversation
     */
    public List<String> getAttachments() {
        return attachments;
    }

    public void setAttachments(List<String> attachments) {
        this.attachments = attachments;
    }
}
