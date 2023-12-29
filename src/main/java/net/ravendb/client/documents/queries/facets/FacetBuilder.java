package net.ravendb.client.documents.queries.facets;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public class FacetBuilder<T> implements IFacetBuilder<T>, IFacetOperations<T> {

    private GenericRangeFacet _range;
    private Facet _default;

    private static final Set<String> _rqlKeywords = new TreeSet<>(String::compareToIgnoreCase);

    static {
        _rqlKeywords.add("as");
        _rqlKeywords.add("select");
        _rqlKeywords.add("where");
        _rqlKeywords.add("load");
        _rqlKeywords.add("group");
        _rqlKeywords.add("order");
        _rqlKeywords.add("include");
        _rqlKeywords.add("update");
    }

    @Override
    public IFacetOperations<T> byRanges(RangeBuilder range, RangeBuilder... ranges) {
        if (range == null) {
            throw new IllegalArgumentException("Range cannot be null");
        }

        if (_range == null) {
            _range = new GenericRangeFacet();
        }

        _range.getRanges().add(range);

        if (ranges != null) {
            for (RangeBuilder p : ranges) {
                _range.getRanges().add(p);
            }
        }

        return this;
    }

    @Override
    public IFacetOperations<T> byField(String fieldName) {
        if (_default == null) {
            _default = new Facet();
        }

        if (_rqlKeywords.contains(fieldName)) {
            fieldName = "'" + fieldName + "'";
        }

        _default.setFieldName(fieldName);

        return this;
    }

    @Override
    public IFacetOperations<T> allResults() {
        if (_default == null) {
            _default = new Facet();
        }

        _default.setFieldName(null);
        return this;
    }

    @Override
    public IFacetOperations<T> withOptions(FacetOptions options) {
        FacetBase facet = getFacet();
        if (facet instanceof Facet) {
            ((Facet)facet).setOptions(options);
        }
        return this;
    }

    @Override
    public IFacetOperations<T> withDisplayName(String displayName) {
        getFacet().setDisplayFieldName(displayName);
        return this;
    }

    @Override
    public IFacetOperations<T> sumOn(String path) {
        return sumOn(path, null);
    }

    @Override
    public IFacetOperations<T> sumOn(String path, String displayName) {
        Map<FacetAggregation, Set<FacetAggregationField>> aggregationsMap = getFacet().getAggregations();
        Set<FacetAggregationField> aggregations = aggregationsMap.computeIfAbsent(FacetAggregation.SUM, key -> new HashSet<>());

        FacetAggregationField aggregationField = new FacetAggregationField();
        aggregationField.setName(path);
        aggregationField.setDisplayName(displayName);

        aggregations.add(aggregationField);

        return this;
    }

    @Override
    public IFacetOperations<T> minOn(String path) {
        return minOn(path, null);
    }

    @Override
    public IFacetOperations<T> minOn(String path, String displayName) {
        Map<FacetAggregation, Set<FacetAggregationField>> aggregationsMap = getFacet().getAggregations();
        Set<FacetAggregationField> aggregations = aggregationsMap.computeIfAbsent(FacetAggregation.MIN, key -> new HashSet<>());

        FacetAggregationField aggregationField = new FacetAggregationField();
        aggregationField.setName(path);
        aggregationField.setDisplayName(displayName);

        aggregations.add(aggregationField);

        return this;
    }

    @Override
    public IFacetOperations<T> maxOn(String path) {
        return maxOn(path, null);
    }

    @Override
    public IFacetOperations<T> maxOn(String path, String displayName) {
        Map<FacetAggregation, Set<FacetAggregationField>> aggregationsMap = getFacet().getAggregations();
        Set<FacetAggregationField> aggregations = aggregationsMap.computeIfAbsent(FacetAggregation.MAX, key -> new HashSet<>());

        FacetAggregationField aggregationField = new FacetAggregationField();
        aggregationField.setName(path);
        aggregationField.setDisplayName(displayName);

        aggregations.add(aggregationField);

        return this;
    }

    @Override
    public IFacetOperations<T> averageOn(String path) {
        return averageOn(path, null);
    }

    @Override
    public IFacetOperations<T> averageOn(String path, String displayName) {
        Map<FacetAggregation, Set<FacetAggregationField>> aggregationsMap = getFacet().getAggregations();
        Set<FacetAggregationField> aggregations = aggregationsMap.computeIfAbsent(FacetAggregation.AVERAGE, key -> new HashSet<>());

        FacetAggregationField aggregationField = new FacetAggregationField();
        aggregationField.setName(path);
        aggregationField.setDisplayName(displayName);

        aggregations.add(aggregationField);

        return this;
    }

    public FacetBase getFacet() {
        if (_default != null) {
            return _default;
        }

        return _range;
    }

}
