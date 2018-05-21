package net.ravendb.client.documents.queries.moreLikeThis;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import net.ravendb.client.documents.queries.QueryResultBase;

public class MoreLikeThisQueryResult extends QueryResultBase<ArrayNode, ObjectNode> {

    private Long durationInMs;

    /**
     * @return The duration of actually executing the query server side
     */
    public Long getDurationInMs() {
        return durationInMs;
    }

    /**
     * @param durationInMs The duration of actually executing the query server side
     */
    public void setDurationInMs(Long durationInMs) {
        this.durationInMs = durationInMs;
    }
}
