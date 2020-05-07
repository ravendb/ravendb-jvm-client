package net.ravendb.client.documents.queries.timeSeries;

public interface ITimeSeriesQueryBuilder {
    <T extends TimeSeriesQueryResult> T raw(String queryText);
}
