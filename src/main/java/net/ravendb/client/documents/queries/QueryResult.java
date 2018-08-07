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
        queryResult.setTotalResults(getTotalResults());


        /* TODO
          Highlightings = Highlightings?.ToDictionary(pair => pair.Key, x => new Dictionary<string, string[]>(x.Value)),
                Explanations = Explanations?.ToDictionary(x => x.Key, x => x.Value),
                Timings = Timings?.Clone(),
         */
        /* TBD 4.1
        Map<String, Map<String, List<String>>> highlightings = getHighlightings();

        if (highlightings != null) {
            Map<String, Map<String, List<String>>> newHighlights = new HashMap<>();
            for (Map.Entry<String, Map<String, List<String>>> hightlightEntry : getHighlightings().entrySet()) {
                newHighlights.put(hightlightEntry.getKey(), new HashMap<>(hightlightEntry.getValue()));
            }
            queryResult.setHighlightings(highlightings);
        }*/


        queryResult.setLastQueryTime(getLastQueryTime());
        queryResult.setDurationInMs(getDurationInMs());
        queryResult.setResultEtag(getResultEtag());
        queryResult.setNodeTag(getNodeTag());
        queryResult.setCounterIncludes(getCounterIncludes());
        queryResult.setIncludedCounterNames(getIncludedCounterNames());
        return queryResult;
    }
}
