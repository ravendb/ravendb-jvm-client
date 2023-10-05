package net.ravendb.client.documents.session;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import net.ravendb.client.Constants;
import net.ravendb.client.documents.IdTypeAndName;
import net.ravendb.client.documents.commands.batches.CommandType;
import net.ravendb.client.documents.commands.batches.ICommandData;
import net.ravendb.client.documents.commands.batches.IncrementalTimeSeriesBatchCommandData;
import net.ravendb.client.documents.commands.batches.TimeSeriesBatchCommandData;
import net.ravendb.client.documents.operations.timeSeries.*;
import net.ravendb.client.documents.session.loaders.ITimeSeriesIncludeBuilder;
import net.ravendb.client.documents.session.timeSeries.TimeSeriesEntry;
import net.ravendb.client.primitives.DatesComparator;
import net.ravendb.client.primitives.Reference;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static net.ravendb.client.primitives.DatesComparator.*;

/**
 * Abstract implementation for in memory session operations
 */
public class SessionTimeSeriesBase {

    protected String docId;
    protected String name;
    protected InMemoryDocumentSessionOperations session;

    protected SessionTimeSeriesBase(InMemoryDocumentSessionOperations session, String documentId, String name) {
        if (documentId == null) {
            throw new IllegalArgumentException("DocumentId cannot be null");
        }

        if (name == null) {
            throw new IllegalArgumentException("Name cannot be null");
        }

        this.docId = documentId;
        this.name = name;
        this.session = session;
    }

    protected SessionTimeSeriesBase(InMemoryDocumentSessionOperations session, Object entity, String name) {
        if (entity == null) {
            throw new IllegalArgumentException("Entity cannot be null");
        }
        DocumentInfo documentInfo = session.documentsByEntity.get(entity);
        if (documentInfo == null) {
            throwEntityNotInSession();
            return;
        }

        if (StringUtils.isBlank(name)) {
            throw new IllegalArgumentException("Name cannot be null or whitespace");
        }

        this.docId = documentInfo.getId();
        this.name = name;
        this.session = session;
    }

    public void append(Date timestamp, double value) {
        append(timestamp, value, null);
    }

    public void append(Date timestamp, double value, String tag) {
        append(timestamp, new double[] { value }, tag);
    }

    public void append(Date timestamp, double[] values) {
        append(timestamp, values, null);
    }

    public void append(Date timestamp, double[] values, String tag) {
        DocumentInfo documentInfo = session.documentsById.getValue(docId);
        if (documentInfo != null && session.deletedEntities.contains(documentInfo.getEntity())) {
            throwDocumentAlreadyDeletedInSession(docId, name);
        }

        TimeSeriesOperation.AppendOperation op = new TimeSeriesOperation.AppendOperation(timestamp, values, tag);

        ICommandData command = session.deferredCommandsMap.get(IdTypeAndName.create(docId, CommandType.TIME_SERIES, name));
        if (command != null) {
            TimeSeriesBatchCommandData tsCmd = (TimeSeriesBatchCommandData) command;

            tsCmd.getTimeSeries().append(op);
        } else {
            List<TimeSeriesOperation.AppendOperation> appends = new ArrayList<>();
            appends.add(op);
            session.defer(new TimeSeriesBatchCommandData(docId, name, appends, null));
        }
    }

    public void delete() {
        delete(null, null);
    }

    public void delete(Date at) {
        delete(at, at);
    }

    public void delete(Date from, Date to) {
        DocumentInfo documentInfo = session.documentsById.getValue(docId);
        if (documentInfo != null && session.deletedEntities.contains(documentInfo.getEntity())) {
            throwDocumentAlreadyDeletedInSession(docId, name);
        }

        TimeSeriesOperation.DeleteOperation op = new TimeSeriesOperation.DeleteOperation(from, to);

        ICommandData command = session.deferredCommandsMap.get(IdTypeAndName.create(docId, CommandType.TIME_SERIES, name));
        if (command != null) {
            TimeSeriesBatchCommandData tsCmd = (TimeSeriesBatchCommandData) command;

            tsCmd.getTimeSeries().delete(op);
        } else {
            List<TimeSeriesOperation.DeleteOperation> deletes = new ArrayList<>();
            deletes.add(op);
            session.defer(new TimeSeriesBatchCommandData(docId, name, null, deletes));
        }

        removeFromCacheIfNeeded(from, to);
    }

    private void removeFromCacheIfNeeded(Date from, Date to) {
        Map<String, List<TimeSeriesRangeResult>> cache = session.getTimeSeriesByDocId().get(docId);
        if (cache == null) {
            return;
        }

        if (from == null && to == null) {
            cache.remove(name);
            return;
        }

        List<TimeSeriesRangeResult> ranges = cache.get(name);
        if (ranges != null && !ranges.isEmpty()) {
            ranges.removeIf(range -> compare(leftDate(range.getFrom()), leftDate(from)) <= 0 && compare(rightDate(range.getTo()), rightDate(to)) >= 0);
        }
    }

    public void increment(Date timestamp, double[] values) {
        DocumentInfo documentInfo = session.documentsById.getValue(docId);
        if (documentInfo != null && session.deletedEntities.contains(documentInfo.getEntity())) {
            throwDocumentAlreadyDeletedInSession(docId, name);
        }

        TimeSeriesOperation.IncrementOperation op = new TimeSeriesOperation.IncrementOperation();
        op.setTimestamp(timestamp);
        op.setValues(values);

        ICommandData command = session.deferredCommandsMap.get(IdTypeAndName.create(docId, CommandType.TIME_SERIES_WITH_INCREMENTS, name));
        if (command != null) {
            IncrementalTimeSeriesBatchCommandData tsCmd = (IncrementalTimeSeriesBatchCommandData) command;
            tsCmd.getTimeSeries().increment(op);
        } else {
            List<TimeSeriesOperation.IncrementOperation> list = new ArrayList<>();
            list.add(op);
            session.defer(new IncrementalTimeSeriesBatchCommandData(docId, name, list));
        }
    }

    public void increment(double[] values) {
        increment(new Date(), values);
    }

    public void increment(Date timestamp, double value) {
        increment(timestamp, new double[] { value });
    }

    public void increment(double value) {
        increment(new Date(), value);
    }

    private static void throwDocumentAlreadyDeletedInSession(String documentId, String timeSeries) {
        throw new IllegalStateException("Can't modify timeseries " + timeSeries + " of document " + documentId
                + ", the document was already deleted in this session.");
    }

    protected void throwEntityNotInSession() {
        throw new IllegalArgumentException("Entity is not associated with the session, cannot perform timeseries operations to it. " +
                "Use documentId instead or track the entity in the session.");
    }

    public TimeSeriesEntry[] getTimeSeriesAndIncludes(Date from, Date to, Consumer<ITimeSeriesIncludeBuilder> includes, int start, int pageSize) {
        if (pageSize == 0) {
            return new TimeSeriesEntry[0];
        }

        DocumentInfo document = session.documentsById.getValue(docId);
        if (document != null) {
            JsonNode metadataTimeSeriesRaw = document.getMetadata().get(Constants.Documents.Metadata.TIME_SERIES);
            if (metadataTimeSeriesRaw != null && metadataTimeSeriesRaw.isArray()) {
                ArrayNode metadataTimeSeries = (ArrayNode) metadataTimeSeriesRaw;
                List<String> timeSeries = session.mapper.convertValue(metadataTimeSeries,
                        session.mapper.getTypeFactory().constructCollectionType(List.class, String.class));
                if (timeSeries.stream().noneMatch(name::equalsIgnoreCase)) {
                    // the document is loaded in the session, but the metadata says that there is no such timeseries
                    return new TimeSeriesEntry[0];
                }
            }
        }

        session.incrementRequestCount();

        TimeSeriesRangeResult rangeResult = session.getOperations().send(new GetTimeSeriesOperation(docId, name, from, to, start, pageSize, includes), session.sessionInfo);

        if (rangeResult == null) {
            return null;
        }

        if (!session.noTracking) {
            handleIncludes(rangeResult);

            Map<String, List<TimeSeriesRangeResult>> cache = session.getTimeSeriesByDocId()
                    .computeIfAbsent(docId, k -> new TreeMap<>(String::compareToIgnoreCase));

            List<TimeSeriesRangeResult> ranges = cache.get(name);
            if (ranges != null && ranges.size() > 0) {
                // update
                int index = compare(leftDate(ranges.get(0).getFrom()), rightDate(to)) > 0 ? 0 : ranges.size();
                ranges.add(index, rangeResult);
            } else {
                List<TimeSeriesRangeResult> item = new ArrayList<>();
                item.add(rangeResult);
                cache.put(name, item);
            }
        }

        return rangeResult.getEntries();
    }

    private void handleIncludes(TimeSeriesRangeResult rangeResult) {
        if (rangeResult.getIncludes() == null) {
            return;
        }

        session.registerIncludes(rangeResult.getIncludes());

        rangeResult.setIncludes(null);
    }

    private static List<TimeSeriesEntry> skipAndTrimRangeIfNeeded(Date from, Date to, TimeSeriesRangeResult fromRange,
                                                                  TimeSeriesRangeResult toRange, List<TimeSeriesEntry> values,
                                                                  int skip, int trim) {
        if (fromRange != null && DatesComparator.compare(rightDate(fromRange.getTo()), leftDate(from)) >= 0) {
            // need to skip a part of the first range
            if (toRange != null && DatesComparator.compare(leftDate(toRange.getFrom()), rightDate(to)) <= 0) {
                // also need to trim a part of the last range
                return values.stream().skip(skip).limit(values.size() - skip - trim).collect(Collectors.toList());
            }

            return values.stream().skip(skip).collect(Collectors.toList());
        }

        if (toRange != null && DatesComparator.compare(leftDate(toRange.getFrom()), rightDate(to)) <= 0) {
            // trim a part of the last range

            return values.stream().limit(values.size() - trim).collect(Collectors.toList());
        }

        return values;
    }

    protected List<TimeSeriesEntry> serveFromCache(
            Date from, Date to, int start, int pageSize, Consumer<ITimeSeriesIncludeBuilder> includes) {
        Map<String, List<TimeSeriesRangeResult>> cache = session.getTimeSeriesByDocId().get(docId);
        List<TimeSeriesRangeResult> ranges = cache.get(name);

        // try to find a range in cache that contains [from, to]
        // if found, chop just the relevant part from it and return to the user.

        // otherwise, try to find two ranges (fromRange, toRange),
        // such that 'fromRange' is the last occurence for which range.From <= from
        // and 'toRange' is the first occurence for which range.To >= to.
        // At the same time, figure out the missing partial ranges that we need to get from the server.

        int toRangeIndex;
        int fromRangeIndex = -1;

        List<TimeSeriesRange> rangesToGetFromServer = null;

        for (toRangeIndex = 0; toRangeIndex < ranges.size(); toRangeIndex++) {
            if (DatesComparator.compare(leftDate(ranges.get(toRangeIndex).getFrom()), leftDate(from)) <= 0) {
                if (DatesComparator.compare(rightDate(ranges.get(toRangeIndex).getTo()), rightDate(to)) >= 0
                    || (ranges.get(toRangeIndex).getEntries().length - start >= pageSize)) {
                    // we have the entire range in cache
                    // we have all the range we need
                    // or that we have all the results we need in smaller range

                    return chopRelevantRange(ranges.get(toRangeIndex), from, to, start, pageSize);
                }

                fromRangeIndex = toRangeIndex;
                continue;
            }

            // can't get the entire range from cache
            if (rangesToGetFromServer == null) {
                rangesToGetFromServer = new ArrayList<>();
            }

            // add the missing part [f, t] between current range start (or 'from')
            // and previous range end (or 'to') to the list of ranges we need to get from server

            Date fromToUse = toRangeIndex == 0 || DatesComparator.compare(rightDate(ranges.get(toRangeIndex - 1).getTo()), leftDate(from)) < 0
                    ? from
                    : ranges.get(toRangeIndex - 1).getTo();
            Date toToUse = DatesComparator.compare(leftDate(ranges.get(toRangeIndex).getFrom()), rightDate(to)) <= 0
                    ? ranges.get(toRangeIndex).getFrom()
                    : to;

            rangesToGetFromServer.add(new TimeSeriesRange(name, fromToUse, toToUse));

            if (DatesComparator.compare(rightDate(ranges.get(toRangeIndex).getTo()), rightDate(to)) >= 0) {
                break;
            }
        }

        if (toRangeIndex == ranges.size()) {
            // requested range [from, to] ends after all ranges in cache
            // add the missing part between the last range end and 'to'
            // to the list of ranges we need to get from server

            if (rangesToGetFromServer == null) {
                rangesToGetFromServer = new ArrayList<>();
            }

            rangesToGetFromServer.add(new TimeSeriesRange(name, ranges.get(ranges.size() - 1).getTo(), to));
        }

        // get all the missing parts from server

        session.incrementRequestCount();

        TimeSeriesDetails details = session.getOperations().send(new GetMultipleTimeSeriesOperation(docId, rangesToGetFromServer, start, pageSize, includes), session.sessionInfo);

        if (includes != null) {
            registerIncludes(details);
        }
        // merge all the missing parts we got from server
        // with all the ranges in cache that are between 'fromRange' and 'toRange'

        Reference<List<TimeSeriesEntry>> resultToUserRef = new Reference<>();
        TimeSeriesEntry[] mergedValues = mergeRangesWithResults(from, to, ranges, fromRangeIndex, toRangeIndex, details.getValues().get(name), resultToUserRef);
        List<TimeSeriesEntry> resultToUser = resultToUserRef.value;

        if (!session.noTracking) {
            from = details
                    .getValues()
                    .get(name)
                    .stream()
                    .map(x -> x.getFrom())
                    .filter(Objects::nonNull)
                    .min(Date::compareTo)
                    .orElse(null);
            to = details
                    .getValues()
                    .get(name)
                    .stream()
                    .map(x -> x.getTo())
                    .filter(Objects::nonNull)
                    .max(Date::compareTo)
                    .orElse(null);
            InMemoryDocumentSessionOperations.addToCache(name, from, to,
                    fromRangeIndex, toRangeIndex, ranges, cache, mergedValues);
        }

        return resultToUser;
    }

    private void registerIncludes(TimeSeriesDetails details) {
        for (TimeSeriesRangeResult rangeResult : details.getValues().get(name)) {
            handleIncludes(rangeResult);
        }
    }

    private static TimeSeriesEntry[] mergeRangesWithResults(Date from, Date to, List<TimeSeriesRangeResult> ranges,
                                                                int fromRangeIndex, int toRangeIndex,
                                                                List<TimeSeriesRangeResult> resultFromServer,
                                                                Reference<List<TimeSeriesEntry>> resultToUserRef) {
        int skip = 0;
        int trim = 0;
        int currentResultIndex = 0;
        List<TimeSeriesEntry> mergedValues = new ArrayList<>();

        int start = fromRangeIndex != -1 ? fromRangeIndex : 0;
        int end = toRangeIndex == ranges.size() ? ranges.size() - 1 : toRangeIndex;

        for (int i = start; i <= end; i++) {
            if (i == fromRangeIndex) {
                if (DatesComparator.compare(leftDate(ranges.get(i).getFrom()), leftDate(from)) <= 0 &&
                        DatesComparator.compare(leftDate(from), rightDate(ranges.get(i).getTo())) <= 0) {
                    // requested range [from, to] starts inside 'fromRange'
                    // i.e fromRange.From <= from <= fromRange.To
                    // so we might need to skip a part of it when we return the
                    // result to the user (i.e. skip [fromRange.From, from])

                    if (ranges.get(i).getEntries() != null) {
                        for (TimeSeriesEntry v : ranges.get(i).getEntries()) {
                            mergedValues.add(v);
                            if (DatesComparator.compare(definedDate(v.getTimestamp()), leftDate(from)) < 0) {
                                skip++;
                            }

                        }
                    }
                }

                continue;
            }

            if (currentResultIndex < resultFromServer.size()
                    && DatesComparator.compare(leftDate(resultFromServer.get(currentResultIndex).getFrom()), leftDate(ranges.get(i).getFrom())) < 0) {
                // add current result from server to the merged list
                // in order to avoid duplication, skip first item in range
                // (unless this is the first time we're adding to the merged list)
                List<TimeSeriesEntry> toAdd = Arrays.stream(resultFromServer.get(currentResultIndex++)
                        .getEntries())
                        .skip(mergedValues.size() == 0 ? 0 : 1)
                        .collect(Collectors.toList());
                mergedValues.addAll(toAdd);
            }

            if (i == toRangeIndex) {
                if (DatesComparator.compare(leftDate(ranges.get(i).getFrom()), rightDate(to)) <= 0) {
                    // requested range [from, to] ends inside 'toRange'
                    // so we might need to trim a part of it when we return the
                    // result to the user (i.e. trim [to, toRange.to])

                    for (int index = mergedValues.size() == 0 ? 0 : 1; index < ranges.get(i).getEntries().length; index++) {
                        mergedValues.add(ranges.get(i).getEntries()[index]);
                        if (DatesComparator.compare(definedDate(ranges.get(i).getEntries()[index].getTimestamp()), rightDate(to)) > 0) {
                            trim++;
                        }
                    }
                }

                continue;
            }

            // add current range from cache to the merged list.
            // in order to avoid duplication, skip first item in range if needed
            boolean shouldSkip = false;
            if (!mergedValues.isEmpty()) {
                shouldSkip = ranges.get(i).getEntries()[0].getTimestamp().equals(mergedValues.get(mergedValues.size() - 1).getTimestamp());
            }
            List<TimeSeriesEntry> toAdd = Arrays.stream(ranges.get(i)
                    .getEntries())
                    .skip(!shouldSkip ? 0 : 1)
                    .collect(Collectors.toList());

            mergedValues.addAll(toAdd);
        }

        if (currentResultIndex < resultFromServer.size()) {
            // the requested range ends after all the ranges in cache,
            // so the last missing part is from server
            // add last missing part to the merged list

            List<TimeSeriesEntry> toAdd = Arrays.stream(resultFromServer.get(currentResultIndex++)
                    .getEntries())
                    .skip(mergedValues.size() == 0 ? 0 : 1)
                    .collect(Collectors.toList());
            mergedValues.addAll(toAdd);
        }

        resultToUserRef.value = skipAndTrimRangeIfNeeded(from, to,
                fromRangeIndex == -1 ? null : ranges.get(fromRangeIndex),
                toRangeIndex == ranges.size() ? null : ranges.get(toRangeIndex),
                mergedValues, skip, trim);

        return mergedValues.toArray(new TimeSeriesEntry[0]);
    }

    private static List<TimeSeriesEntry> chopRelevantRange(TimeSeriesRangeResult range, Date from, Date to, int start, int pageSize) {
        if (range.getEntries() == null) {
            return Collections.emptyList();
        }

        List<TimeSeriesEntry> result = new ArrayList<>();
        for (TimeSeriesEntry value : range.getEntries()) {
            if (DatesComparator.compare(definedDate(value.getTimestamp()), rightDate(to)) > 0) {
                break;
            }
            if (DatesComparator.compare(definedDate(value.getTimestamp()), leftDate(from)) < 0) {
                continue;
            }
            if (start-- > 0) {
                continue;
            }

            if (pageSize-- <= 0) {
                break;
            }

            result.add(value);
        }

        return result;
    }

    protected <T> TimeSeriesEntry[] getFromCache(Date from, Date to, Consumer<ITimeSeriesIncludeBuilder> includes, int start, int pageSize) {
        // RavenDB-16060
        // Typed TimeSeries results need special handling when served from cache
        // since we cache the results untyped

        // in java we return untyped entries here

        List<TimeSeriesEntry> resultToUser = serveFromCache(from, to, start, pageSize, includes);
        if (resultToUser.isEmpty()) {
            return new TimeSeriesEntry[0];
        }

        return resultToUser.toArray(new TimeSeriesEntry[0]);
    }

    protected boolean notInCache(Date from, Date to) {
        Map<String, List<TimeSeriesRangeResult>> cache = session.getTimeSeriesByDocId().get(docId);
        if (cache == null) {
            return true;
        }

        List<TimeSeriesRangeResult> ranges = cache.get(name);
        if (ranges == null) {
            return true;
        }

        return ranges.isEmpty()
                || DatesComparator.compare(leftDate(ranges.get(0).getFrom()), rightDate(to)) > 0
                || DatesComparator.compare(rightDate(ranges.get(ranges.size() - 1).getTo()), leftDate(from)) < 0;
    }

    private static class CachedEntryInfo {
        public boolean servedFromCache;
        public List<TimeSeriesEntry> resultToUser;
        public TimeSeriesEntry[] mergedValues;
        public int fromRangeIndex;
        public int toRangeIndex;

        public CachedEntryInfo(boolean servedFromCache, List<TimeSeriesEntry> resultToUser, TimeSeriesEntry[] mergedValues, int fromRangeIndex, int toRangeIndex) {
            this.servedFromCache = servedFromCache;
            this.resultToUser = resultToUser;
            this.mergedValues = mergedValues;
            this.fromRangeIndex = fromRangeIndex;
            this.toRangeIndex = toRangeIndex;
        }
    }
}
