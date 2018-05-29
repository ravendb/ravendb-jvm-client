package net.ravendb.client.documents.queries.facets;

import com.fasterxml.jackson.annotation.JsonProperty;
import net.ravendb.client.documents.session.tokens.FacetToken;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class RangeFacet extends FacetBase {

    private FacetBase _parent;

    @JsonProperty("Ranges")
    private List<String> ranges;

    public RangeFacet(FacetBase parent) {
        this();
        _parent = parent;
    }

    public RangeFacet() {
        ranges = new ArrayList<>();
    }

    public List<String> getRanges() {
        return ranges;
    }

    public void setRanges(List<String> ranges) {
        this.ranges = ranges;
    }

    @Override
    public FacetToken toFacetToken(Function<Object, String> addQueryParameter) {
        if (_parent != null) {
            return _parent.toFacetToken(addQueryParameter);
        }

        return FacetToken.create(this, addQueryParameter);
    }
}
