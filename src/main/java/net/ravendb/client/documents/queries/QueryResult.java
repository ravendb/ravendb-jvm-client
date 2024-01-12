package net.ravendb.client.documents.queries;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.HashMap;

public class QueryResult extends GenericQueryResult<ArrayNode, ObjectNode> {

    /**
     * Creates a snapshot of the query results
     * @return returns snapshot of query result
     */
    public QueryResult createSnapshot() {
        QueryResult queryResult = new QueryResult();
        queryResult.setResults(getResults());
        queryResult.setIncludes(getIncludes());
        queryResult.setIndexName(getIndexName());
        queryResult.setIndexTimestamp(getIndexTimestamp());
        queryResult.setIncludedPaths(getIncludedPaths());
        queryResult.setStale(isStale());
        queryResult.setSkippedResults(getSkippedResults());
        queryResult.setScannedResults(getScannedResults());
        queryResult.setTotalResults(getTotalResults());
        queryResult.setHighlightings(getHighlightings() != null ? new HashMap<>(getHighlightings()) : null);
        queryResult.setExplanations(getExplanations() != null ? new HashMap<>(getExplanations()) : null);
        queryResult.setTimings(getTimings());
        queryResult.setLastQueryTime(getLastQueryTime());
        queryResult.setDurationInMs(getDurationInMs());
        queryResult.setResultEtag(getResultEtag());
        queryResult.setNodeTag(getNodeTag());
        queryResult.setCounterIncludes(getCounterIncludes());
        queryResult.setRevisionIncludes(getRevisionIncludes());
        queryResult.setIncludedCounterNames(getIncludedCounterNames());
        queryResult.setTimeSeriesIncludes(getTimeSeriesIncludes());
        queryResult.setCompareExchangeValueIncludes(getCompareExchangeValueIncludes());
        return queryResult;
    }
}
