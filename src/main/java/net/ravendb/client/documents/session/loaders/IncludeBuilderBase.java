package net.ravendb.client.documents.session.loaders;

import net.ravendb.client.Constants;
import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.operations.timeSeries.*;
import net.ravendb.client.primitives.TimeValue;
import net.ravendb.client.primitives.Tuple;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.CompareToBuilder;

import java.util.*;

public class IncludeBuilderBase {

    protected int nextParameterId = 1;

    protected final DocumentConventions _conventions;
    public Set<String> documentsToInclude;

    public String alias;
    public Map<String, Tuple<Boolean, Set<String>>> countersToIncludeBySourcePath;
    public Map<String, Set<AbstractTimeSeriesRange>> timeSeriesToIncludeBySourceAlias;
    public Set<String> compareExchangeValuesToInclude;
    public boolean includeTimeSeriesTags;
    public boolean includeTimeSeriesDocument;

    public Set<AbstractTimeSeriesRange> getTimeSeriesToInclude() {
        if (timeSeriesToIncludeBySourceAlias == null) {
            return null;
        }

        return timeSeriesToIncludeBySourceAlias.get("");
    }

    public Set<String> getCountersToInclude() {
        if (countersToIncludeBySourcePath == null) {
            return null;
        }

        Tuple<Boolean, Set<String>> value = countersToIncludeBySourcePath.get("");

        return value != null ? value.second : new HashSet<>();
    }

    public boolean isAllCounters() {
        if (countersToIncludeBySourcePath == null) {
            return false;
        }

        Tuple<Boolean, Set<String>> value = countersToIncludeBySourcePath.get("");
        return value != null ? value.first : false;
    }

    public IncludeBuilderBase(DocumentConventions conventions) {
        _conventions = conventions;
    }

    protected void _includeCompareExchangeValue(String path) {
        if (compareExchangeValuesToInclude == null) {
            compareExchangeValuesToInclude = new HashSet<>();
        }

        compareExchangeValuesToInclude.add(path);
    }

    protected void _includeCounterWithAlias(String path, String name) {
        _withAlias();
        _includeCounter(path, name);
    }

    protected void _includeCounterWithAlias(String path, String[] names) {
        _withAlias();
        _includeCounters(path, names);
    }

    protected void _includeDocuments(String path) {
        if (documentsToInclude == null) {
            documentsToInclude = new HashSet<>();
        }

        documentsToInclude.add(path);
    }

    protected void _includeCounter(String path, String name) {
        if (StringUtils.isEmpty(name)) {
            throw new IllegalArgumentException("Name cannot be empty");
        }

        assertNotAllAndAddNewEntryIfNeeded(path);

        countersToIncludeBySourcePath.get(path).second.add(name);
    }

    protected void _includeCounters(String path, String[] names) {
        if (names == null) {
            throw new IllegalArgumentException("Names cannot be null");
        }

        assertNotAllAndAddNewEntryIfNeeded(path);

        for (String name : names) {
            if (StringUtils.isBlank(name)) {
                throw new IllegalArgumentException("Counters(String[] names): 'names' should not contain null or whitespace elements");
            }

            countersToIncludeBySourcePath.get(path).second.add(name);
        }
    }

    protected void _includeAllCountersWithAlias(String path) {
        _withAlias();
        _includeAllCounters(path);
    }

    protected void _includeAllCounters(String sourcePath) {
        if (countersToIncludeBySourcePath == null) {
            countersToIncludeBySourcePath = new TreeMap<>(String::compareToIgnoreCase);
        }

        Tuple<Boolean, Set<String>> val = countersToIncludeBySourcePath.get(sourcePath);

        if (val != null && val.second != null) {
            throw new IllegalStateException("You cannot use allCounters() after using counter(String name) or counters(String[] names)");
        }

        countersToIncludeBySourcePath.put(sourcePath, Tuple.create(true, null));
    }

    protected void assertNotAllAndAddNewEntryIfNeeded(String path) {
        if (countersToIncludeBySourcePath != null) {
            Tuple<Boolean, Set<String>> val = countersToIncludeBySourcePath.get(path);
            if (val != null && val.first) {
                throw new IllegalStateException("You cannot use counter(name) after using allCounters()");
            }
        }

        if (countersToIncludeBySourcePath == null) {
            countersToIncludeBySourcePath = new TreeMap<>(String::compareToIgnoreCase);
        }

        if (!countersToIncludeBySourcePath.containsKey(path)) {
            countersToIncludeBySourcePath.put(path, Tuple.create(false, new TreeSet<>(String::compareToIgnoreCase)));
        }
    }

    protected void _withAlias() {
        if (alias == null) {
            alias = "a_" + (nextParameterId++);
        }
    }

    protected void _includeTimeSeriesFromTo(String alias, String name, Date from, Date to) {
        assertValid(alias, name);

        if (timeSeriesToIncludeBySourceAlias == null) {
            timeSeriesToIncludeBySourceAlias = new HashMap<>();
        }

        Set<AbstractTimeSeriesRange> hashSet = timeSeriesToIncludeBySourceAlias.computeIfAbsent(alias, (key) -> new TreeSet<>(AbstractTimeSeriesRangeComparer.INSTANCE));

        TimeSeriesRange range = new TimeSeriesRange();
        range.setName(name);
        range.setFrom(from);
        range.setTo(to);

        hashSet.add(range);
    }

    protected void _includeTimeSeriesByRangeTypeAndTime(String alias, String name, TimeSeriesRangeType type, TimeValue time) {
        assertValid(alias, name);
        assertValidType(type, time);

        if (timeSeriesToIncludeBySourceAlias == null) {
            timeSeriesToIncludeBySourceAlias = new HashMap<>();
        }

        Set<AbstractTimeSeriesRange> hashSet = timeSeriesToIncludeBySourceAlias
                .computeIfAbsent(alias, a -> new TreeSet<>(AbstractTimeSeriesRangeComparer.INSTANCE));

        TimeSeriesTimeRange timeRange = new TimeSeriesTimeRange();
        timeRange.setName(name);
        timeRange.setTime(time);
        timeRange.setType(type);
        hashSet.add(timeRange);
    }

    private static void assertValidType(TimeSeriesRangeType type, TimeValue time) {
        switch (type) {
            case NONE:
                throw new IllegalArgumentException("Time range type cannot be set to NONE when time is specified.");
            case LAST:
                if (time.compareTo(TimeValue.ZERO) != 0) {
                    if (time.getValue() <= 0) {
                        throw new IllegalArgumentException("Time range type cannot be set to LAST when time is negative or zero.");
                    }

                    return;
                }

                throw new IllegalArgumentException("time range type cannot be set to LAST when time is not specified.");
            default:
                throw new UnsupportedOperationException("Not supported time range type: " + type);
        }
    }

    protected void _includeTimeSeriesByRangeTypeAndCount(String alias, String name, TimeSeriesRangeType type, int count) {
        assertValid(alias, name);
        assertValidTypeAndCount(type, count);

        if (timeSeriesToIncludeBySourceAlias == null) {
            timeSeriesToIncludeBySourceAlias = new HashMap<>();
        }

        Set<AbstractTimeSeriesRange> hashSet = timeSeriesToIncludeBySourceAlias.computeIfAbsent(alias, a -> new TreeSet<>(AbstractTimeSeriesRangeComparer.INSTANCE));

        TimeSeriesCountRange countRange = new TimeSeriesCountRange();
        countRange.setName(name);
        countRange.setCount(count);
        countRange.setType(type);

        hashSet.add(countRange);
    }

    private static void assertValidTypeAndCount(TimeSeriesRangeType type, int count) {
        switch (type) {
            case NONE:
                throw new IllegalArgumentException("Time range type cannot be set to NONE when count is specified.");
            case LAST:
                if (count <= 0) {
                    throw new IllegalArgumentException("Count have to be positive.");
                }
                break;
            default:
                throw new UnsupportedOperationException("Not supported time range type: " + type);
        }
    }

    protected void _includeArrayOfTimeSeriesByRangeTypeAndTime(String[] names, TimeSeriesRangeType type, TimeValue time) {
        if (names == null) {
            throw new IllegalArgumentException("Names cannot be null");
        }

        for (String name : names) {
            _includeTimeSeriesByRangeTypeAndTime("", name, type, time);
        }
    }

    protected void _includeArrayOfTimeSeriesByRangeTypeAndCount(String[] names, TimeSeriesRangeType type, int count) {
        if (names == null) {
            throw new IllegalArgumentException("Names cannot be null");
        }

        for (String name : names) {
            _includeTimeSeriesByRangeTypeAndCount("", name, type, count);
        }
    }

    private void assertValid(String alias, String name) {
        if (StringUtils.isBlank(name)) {
            throw new IllegalArgumentException("Name cannot be null or whitespace.");
        }

        if (timeSeriesToIncludeBySourceAlias != null) {
            Set<AbstractTimeSeriesRange> hashSet2 = timeSeriesToIncludeBySourceAlias.get(alias);
            if (hashSet2 != null && !hashSet2.isEmpty()) {
                if (Constants.TimeSeries.ALL.equals(name)) {
                    throw new IllegalArgumentException("IIncludeBuilder : Cannot use 'includeAllTimeSeries' after using 'includeTimeSeries' or 'includeAllTimeSeries'.");
                }

                if (hashSet2.stream().anyMatch(x -> Constants.TimeSeries.ALL.equals(x.getName()))) {
                    throw new IllegalArgumentException("IIncludeBuilder : Cannot use 'includeTimeSeries' or 'includeAllTimeSeries' after using 'includeAllTimeSeries'.");
                }
            }
        }
    }

    public Set<String> getCompareExchangeValuesToInclude() {
        return compareExchangeValuesToInclude;
    }

    public static class AbstractTimeSeriesRangeComparer implements Comparator<AbstractTimeSeriesRange> {
        public final static AbstractTimeSeriesRangeComparer INSTANCE = new AbstractTimeSeriesRangeComparer();

        private AbstractTimeSeriesRangeComparer() {
        }

        @Override
        public int compare(AbstractTimeSeriesRange x, AbstractTimeSeriesRange y) {
            String xName = x != null ? x.getName() : null;
            String yName = y != null ? y.getName() : null;

            return new CompareToBuilder()
                    .append(xName, yName)
                    .toComparison();
        }
    }
}
