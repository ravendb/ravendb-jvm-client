package net.ravendb.client.documents.queries;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class QueryResult extends GenericQueryResult<ArrayNode, ObjectNode> {

    /**
     * Creates a snapshot of the query results
     */
    public QueryResult createSnapshot() {
        QueryResult queryResult = new QueryResult();
        queryResult.setResults(getResults());
        queryResult.setIncludes(getIncludes());
        queryResult.setIndexName(getIndexName());
        queryResult.setIndexTimestamp(getIndexTimestamp());
        queryResult.setIncludedPaths(queryResult.getIncludedPaths());
        queryResult.setStale(isStale());
        queryResult.setSkippedResults(getSkippedResults());
        queryResult.setTotalResults(getTotalResults());

        Map<String, Map<String, List<String>>> highlightings = getHighlightings();

        if (highlightings != null) {
            Map<String, Map<String, List<String>>> newHighlights = new HashMap<>();
            for (Map.Entry<String, Map<String, List<String>>> hightlightEntry : getHighlightings().entrySet()) {
                newHighlights.put(hightlightEntry.getKey(), new HashMap<>(hightlightEntry.getValue()));
            }
            queryResult.setHighlightings(highlightings);
        }

        if (getScoreExplanations() != null) {
            queryResult.setScoreExplanations(new HashMap<>(getScoreExplanations()));
        }

        if (getTimingsInMs() != null) {
            queryResult.setTimingsInMs(new HashMap<>(getTimingsInMs()));
        }

        queryResult.setLastQueryTime(getLastQueryTime());
        queryResult.setDurationInMs(getDurationInMs());
        queryResult.setResultEtag(getResultEtag());
        return queryResult;
    }
}
