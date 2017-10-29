package net.ravendb.client.documents.queries;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class QueryResult extends GenericQueryResult<ArrayNode, ObjectNode> {

    public void ensureSnapshot() {

    }

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
        /* TODO
        Highlightings = Highlightings?.ToDictionary(
                    pair => pair.Key,
                    x => new Dictionary<string, string[]>(x.Value)),
                ScoreExplanations = ScoreExplanations?.ToDictionary(x => x.Key, x => x.Value),
                TimingsInMs = TimingsInMs?.ToDictionary(x => x.Key, x => x.Value),
         */
        queryResult.setLastQueryTime(getLastQueryTime());
        queryResult.setDurationInMs(getDurationInMs());
        queryResult.setResultEtag(getResultEtag());
        return queryResult;
    }
}
