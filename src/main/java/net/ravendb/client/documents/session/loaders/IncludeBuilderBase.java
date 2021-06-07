package net.ravendb.client.documents.session.loaders;

import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.operations.timeSeries.AbstractTimeSeriesRange;
import net.ravendb.client.documents.operations.timeSeries.TimeSeriesRange;
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
