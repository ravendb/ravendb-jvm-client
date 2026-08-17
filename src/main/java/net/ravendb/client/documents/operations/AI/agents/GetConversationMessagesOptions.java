package net.ravendb.client.documents.operations.AI.agents;

import org.apache.commons.lang3.StringUtils;

import java.util.Date;

/**
 * Parameters for reading messages from an AI agent conversation.
 */
public class GetConversationMessagesOptions {

    private String conversationId;
    private Date before;
    private Date after;
    private int pageSize = Integer.MAX_VALUE;
    private AiConversationDetailLevel detailLevel = AiConversationDetailLevel.SIMPLE;

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
     * @return return messages older than this timestamp (exclusive upper bound). Used for backward paging
     *         (scrolling up in a chatbot UI).
     */
    public Date getBefore() {
        return before;
    }

    public void setBefore(Date before) {
        this.before = before;
    }

    /**
     * @return return messages newer than this timestamp (exclusive lower bound). Used for catching up on
     *         new messages (e.g. after a changes() notification).
     */
    public Date getAfter() {
        return after;
    }

    public void setAfter(Date after) {
        this.after = after;
    }

    /**
     * @return the maximum number of messages to return. Defaults to {@link Integer#MAX_VALUE}.
     */
    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    /**
     * Gets the level of detail in returned messages.
     *
     * <ul>
     *   <li>{@link AiConversationDetailLevel#SIMPLE} (default): user messages (including attachment-only)
     *       and assistant messages with content.</li>
     *   <li>{@link AiConversationDetailLevel#DETAILED}: adds system messages and tool calls with results.</li>
     *   <li>{@link AiConversationDetailLevel#FULL}: no filtering, includes summaries and internal messages.</li>
     * </ul>
     * @return the detail level
     */
    public AiConversationDetailLevel getDetailLevel() {
        return detailLevel;
    }

    public void setDetailLevel(AiConversationDetailLevel detailLevel) {
        this.detailLevel = detailLevel;
    }

    void validate() {
        if (StringUtils.isEmpty(conversationId)) {
            throw new IllegalArgumentException("ConversationId cannot be null or empty");
        }

        if (before != null && after != null) {
            throw new IllegalArgumentException("Before and After cannot both be specified.");
        }

        if (pageSize <= 0) {
            throw new IllegalArgumentException("PageSize must be greater than 0.");
        }
    }
}
