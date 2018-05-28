package net.ravendb.client.documents.queries.facets;

import org.apache.commons.lang3.ObjectUtils;

import java.util.function.Function;

public class RangeBuilder<T> {
    private final String path;

    private T lessBound;
    private T greaterBound;
    private boolean lessInclusive;
    private boolean greaterInclusive;

    private boolean lessSet = false;
    private boolean greaterSet = false;

    public RangeBuilder(String path) {
        this.path = path;
    }

    public static <T> RangeBuilder<T> forPath(String path) {
        return new RangeBuilder<>(path);
    }

    private RangeBuilder<T> createClone() {
        RangeBuilder<T> builder = new RangeBuilder<>(path);
        builder.lessBound = lessBound;
        builder.greaterBound = greaterBound;
        builder.lessInclusive = lessInclusive;
        builder.greaterInclusive = greaterInclusive;
        builder.lessSet = lessSet;
        builder.greaterSet = greaterSet;
        return builder;
    }

    public RangeBuilder<T> isLessThan(T value) {
        if (lessSet) {
            throw new IllegalStateException("Less bound was already set");
        }

        RangeBuilder<T> clone = createClone();
        clone.lessBound = value;
        clone.lessInclusive = false;
        clone.lessSet = true;
        return clone;
    }

    public RangeBuilder<T> isLessThanOrEqualTo(T value) {
        if (lessSet) {
            throw new IllegalStateException("Less bound was already set");
        }

        RangeBuilder<T> clone = createClone();
        clone.lessBound = value;
        clone.lessInclusive = true;
        clone.lessSet = true;
        return clone;
    }

    public RangeBuilder<T> isGreaterThan(T value) {
        if (greaterSet) {
            throw new IllegalStateException("Greater bound was already set");
        }

        RangeBuilder<T> clone = createClone();
        clone.greaterBound = value;
        clone.greaterInclusive = false;
        clone.greaterSet = true;
        return clone;
    }

    public RangeBuilder<T> isGreaterThanOrEqualTo(T value) {
        if (greaterSet) {
            throw new IllegalStateException("Greater bound was already set");
        }

        RangeBuilder<T> clone = createClone();
        clone.greaterBound = value;
        clone.greaterInclusive = true;
        clone.greaterSet = true;
        return clone;
    }

    public String getStringRepresentation(Function<Object,String> addQueryParameter) {
        String less = null;
        String greater = null;

        if (!lessSet && !greaterSet) {
            throw new IllegalStateException("Bounds were not set");
        }

        if (lessSet) {
            String lessParamName = addQueryParameter.apply(lessBound);
            less = path + (lessInclusive ? " <= " : " < ") + "$" + lessParamName;
        }

        if (greaterSet) {
            String greaterParamName = addQueryParameter.apply(greaterBound);
            greater = path + (greaterInclusive ? " >= " : " > ") + "$" + greaterParamName;
        }

        if (less != null && greater != null) {
            return greater + " and " + less;
        }

        return ObjectUtils.firstNonNull(less, greater);
    }
}
