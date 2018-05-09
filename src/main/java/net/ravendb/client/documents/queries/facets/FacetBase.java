package net.ravendb.client.documents.queries.facets;

import net.ravendb.client.documents.session.tokens.FacetToken;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public abstract class FacetBase {
    private String displayFieldName;
    private FacetOptions options;
    private Map<FacetAggregation, String> aggregations;

    public FacetBase() {
        aggregations = new HashMap<>();
    }

    public String getDisplayFieldName() {
        return displayFieldName;
    }

    public void setDisplayFieldName(String displayFieldName) {
        this.displayFieldName = displayFieldName;
    }

    public FacetOptions getOptions() {
        return options;
    }

    public void setOptions(FacetOptions options) {
        this.options = options;
    }

    public Map<FacetAggregation, String> getAggregations() {
        return aggregations;
    }

    public void setAggregations(Map<FacetAggregation, String> aggregations) {
        this.aggregations = aggregations;
    }

    public abstract FacetToken toFacetToken(Function<Object, String> addQueryParameter);
}
