package net.ravendb.client.documents.queries.facets;

import net.ravendb.client.documents.session.tokens.FacetToken;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class GenericRangeFacet extends FacetBase {

    private FacetBase _parent;

    private List<RangeBuilder<?>> ranges;

    public GenericRangeFacet(FacetBase parent) {
        this();
        _parent = parent;
    }

    public GenericRangeFacet() {
        ranges = new ArrayList<>();
    }

    public static String parse(RangeBuilder<?> rangeBuilder, Function<Object,String> addQueryParameter) {
        return rangeBuilder.getStringRepresentation(addQueryParameter);
    }

    public List<RangeBuilder<?>> getRanges() {
        return ranges;
    }

    public void setRanges(List<RangeBuilder<?>> ranges) {
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
