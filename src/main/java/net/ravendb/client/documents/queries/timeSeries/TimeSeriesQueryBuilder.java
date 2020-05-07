package net.ravendb.client.documents.queries.timeSeries;

public class TimeSeriesQueryBuilder implements ITimeSeriesQueryBuilder {

    private String _query;

    @Override
    public <T extends TimeSeriesQueryResult> T raw(String queryText) {
        _query = queryText;
        return null;
    }

    public String getQueryText() {
        return _query;
    }
}
