package net.ravendb.client.documents.session.tokens;

import net.ravendb.client.documents.queries.facets.*;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class FacetToken extends QueryToken {

    private String _facetSetupDocumentId;
    private String _aggregateByFieldName;
    private String _alias;
    private List<String> _ranges;
    private String _optionsParameterName;

    private List<FacetAggregationToken> _aggregations;

    public String getName() {
        return ObjectUtils.firstNonNull(_alias, _aggregateByFieldName);
    }

    private FacetToken(String facetSetupDocumentId) {
        _facetSetupDocumentId = facetSetupDocumentId;
    }

    private FacetToken(String aggregateByFieldName, String alias, List<String> ranges, String optionsParameterName) {
        _aggregateByFieldName = aggregateByFieldName;
        _alias = alias;
        _ranges = ranges;
        _optionsParameterName = optionsParameterName;
        _aggregations = new ArrayList<>();
    }

    public static FacetToken create(String facetSetupDocumentId) {
        if (StringUtils.isBlank(facetSetupDocumentId)) {
            throw new IllegalArgumentException("facetSetupDocumentId cannot be null");
        }

        return new FacetToken(facetSetupDocumentId);
    }

    public static FacetToken create(Facet facet, Function<Object, String> addQueryParameter) {
        String optionsParameterName = getOptionsParameterName(facet, addQueryParameter);
        FacetToken token = new FacetToken(facet.getFieldName(), facet.getDisplayFieldName(), null, optionsParameterName);

        applyAggregations(facet, token);

        return token;
    }

    public static FacetToken create(RangeFacet facet, Function<Object, String> addQueryParameter) {
        String optionsParameterName = getOptionsParameterName(facet, addQueryParameter);

        FacetToken token = new FacetToken(null, facet.getDisplayFieldName(), facet.getRanges(), optionsParameterName);

        applyAggregations(facet, token);

        return token;
    }

    public static FacetToken create(GenericRangeFacet facet, Function<Object, String> addQueryParameter) {
        String optionsParameterName = getOptionsParameterName(facet, addQueryParameter);

        List<String> ranges = new ArrayList<>();
        for (RangeBuilder<?> rangeBuilder : facet.getRanges()) {
            ranges.add(GenericRangeFacet.parse(rangeBuilder, addQueryParameter));
        }

        FacetToken token = new FacetToken(null, facet.getDisplayFieldName(), ranges, optionsParameterName);

        applyAggregations(facet, token);
        return token;
    }

    public static FacetToken create(FacetBase facet, Function<Object, String> addQueryParameter) {
        // this is just a dispatcher
        return facet.toFacetToken(addQueryParameter);
    }


    @Override
    public void writeTo(StringBuilder writer) {
        writer.append("facet(");

        if (_facetSetupDocumentId != null) {
            writer
                    .append("id('")
                    .append(_facetSetupDocumentId)
                    .append("'))");

            return;
        }

        boolean firstArgument = false;

        if (_aggregateByFieldName != null) {
            writer.append(_aggregateByFieldName);
        } else if (_ranges != null) {
            boolean firstInRange = true;

            for (String range: _ranges) {
                if (!firstInRange) {
                    writer.append(", ");
                }

                firstInRange = false;
                writer.append(range);
            }
        } else {
            firstArgument = true;
        }

        for (FacetAggregationToken aggregation : _aggregations) {
            if (!firstArgument) {
                writer.append(", ");
            }
            firstArgument = false;
            aggregation.writeTo(writer);
        }

        if (StringUtils.isNotBlank(_optionsParameterName)) {
            writer
                    .append(", $")
                    .append(_optionsParameterName);
        }

        writer.append(")");

        if (StringUtils.isBlank(_alias) || _alias.equals(_aggregateByFieldName)) {
            return;
        }

        writer
                .append(" as ")
                .append(_alias);
    }

    private static void applyAggregations(FacetBase facet, FacetToken token) {
        for (Map.Entry<FacetAggregation, String> aggregation : facet.getAggregations().entrySet()) {
            FacetAggregationToken aggregationToken;
            switch (aggregation.getKey()) {
                case MAX:
                    aggregationToken = FacetAggregationToken.max(aggregation.getValue());
                    break;
                case MIN:
                    aggregationToken = FacetAggregationToken.min(aggregation.getValue());
                    break;
                case AVERAGE:
                    aggregationToken = FacetAggregationToken.average(aggregation.getValue());
                    break;
                case SUM:
                    aggregationToken = FacetAggregationToken.sum(aggregation.getValue());
                    break;
                default :
                    throw new NotImplementedException("Unsupported aggregation method: " + aggregation.getKey());
            }

            token._aggregations.add(aggregationToken);
        }
    }


    private static String getOptionsParameterName(FacetBase facet, Function<Object, String> addQueryParameter) {
        return facet.getOptions() != null && facet.getOptions() != FacetOptions.getDefaultOptions() ? addQueryParameter.apply(facet.getOptions()) : null;
    }

    private static class FacetAggregationToken extends QueryToken {
        private final String _fieldName;
        private final FacetAggregation _aggregation;

        private FacetAggregationToken(String fieldName, FacetAggregation aggregation) {
            _fieldName = fieldName;
            _aggregation = aggregation;
        }

        @Override
        public void writeTo(StringBuilder writer) {
            switch (_aggregation) {
                case MAX:
                    writer
                            .append("max(")
                            .append(_fieldName)
                            .append(")");
                    break;
                case MIN:
                    writer
                            .append("min(")
                            .append(_fieldName)
                            .append(")");
                    break;
                case AVERAGE:
                    writer
                            .append("avg(")
                            .append(_fieldName)
                            .append(")");
                    break;
                case SUM:
                    writer
                            .append("sum(")
                            .append(_fieldName)
                            .append(")");
                    break;
                default:
                    throw new IllegalArgumentException("Invalid aggregation mode: " + _aggregation);
            }
        }

        public static FacetAggregationToken max(String fieldName) {
            if (StringUtils.isBlank(fieldName)) {
                throw new IllegalArgumentException("FieldName can not be null");
            }
            return new FacetAggregationToken(fieldName, FacetAggregation.MAX);
        }

        public static FacetAggregationToken min(String fieldName) {
            if (StringUtils.isBlank(fieldName)) {
                throw new IllegalArgumentException("FieldName can not be null");
            }
            return new FacetAggregationToken(fieldName, FacetAggregation.MIN);
        }

        public static FacetAggregationToken average(String fieldName) {
            if (StringUtils.isBlank(fieldName)) {
                throw new IllegalArgumentException("FieldName can not be null");
            }
            return new FacetAggregationToken(fieldName, FacetAggregation.AVERAGE);
        }

        public static FacetAggregationToken sum(String fieldName) {
            if (StringUtils.isBlank(fieldName)) {
                throw new IllegalArgumentException("FieldName can not be null");
            }
            return new FacetAggregationToken(fieldName, FacetAggregation.SUM);
        }
    }
}
