package net.ravendb.client.documents.session;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import net.ravendb.client.Constants;
import net.ravendb.client.documents.IdTypeAndName;
import net.ravendb.client.documents.commands.batches.CommandType;
import net.ravendb.client.documents.commands.batches.ICommandData;
import net.ravendb.client.documents.commands.batches.TimeSeriesBatchCommandData;
import net.ravendb.client.documents.operations.timeSeries.*;
import net.ravendb.client.documents.session.timeSeries.TimeSeriesEntry;
import net.ravendb.client.primitives.NetISO8601Utils;
import net.ravendb.client.primitives.Reference;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

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
        DocumentInfo documentInfo = session.documentsByEntity.get(entity);
        if (documentInfo == null) {
            throwEntityNotInSession(entity);
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

            if (tsCmd.getTimeSeries().getAppends() == null) {
                tsCmd.getTimeSeries().setAppends(new ArrayList<>());
            }

            tsCmd.getTimeSeries().getAppends().add(op);
        } else {
            List<TimeSeriesOperation.AppendOperation> appends = new ArrayList<>();
            appends.add(op);
            session.defer(new TimeSeriesBatchCommandData(docId, name, appends, null));
        }
    }

    public void remove() {
        remove(null, null);
    }

    public void remove(Date at) {
        remove(at, at);
    }

    public void remove(Date from, Date to) {
        DocumentInfo documentInfo = session.documentsById.getValue(docId);
        if (documentInfo != null && session.deletedEntities.contains(documentInfo.getEntity())) {
            throwDocumentAlreadyDeletedInSession(docId, name);
        }

        TimeSeriesOperation.RemoveOperation op = new TimeSeriesOperation.RemoveOperation(from, to);

        ICommandData command = session.deferredCommandsMap.get(IdTypeAndName.create(docId, CommandType.TIME_SERIES, name));
        if (command != null) {
            TimeSeriesBatchCommandData tsCmd = (TimeSeriesBatchCommandData) command;

            if (tsCmd.getTimeSeries().getRemovals() == null) {
                tsCmd.getTimeSeries().setRemovals(new ArrayList<>());
            }

            tsCmd.getTimeSeries().getRemovals().add(op);
        } else {
            List<TimeSeriesOperation.RemoveOperation> removals = new ArrayList<>();
            removals.add(op);
            session.defer(new TimeSeriesBatchCommandData(docId, name, null, removals));
        }
    }

    private static void throwDocumentAlreadyDeletedInSession(String documentId, String timeSeries) {
        throw new IllegalStateException("Can't modify timeseries " + timeSeries + " of document " + documentId + ", the document was already delete in this session.");
    }

    protected void throwEntityNotInSession(Object entity) {
        throw new IllegalArgumentException("Entity is not associated with the session, cannot add timeseries to it. " +
                "Use documentId instead or track the entity in the session.");
    }

    public List<TimeSeriesEntry> getInternal(Date from, Date to, int start, int pageSize) {
        TimeSeriesRangeResult rangeResult;
        //TODO: ?? - remove and use custom comparator!
        if (from == null) {
            from = NetISO8601Utils.MIN_DATE;
        }
        if (to == null) {
            to = NetISO8601Utils.MAX_DATE;
        }

        Map<String, List<TimeSeriesRangeResult>> cache = session.getTimeSeriesByDocId().get(docId);
        if (cache != null) {
            List<TimeSeriesRangeResult> ranges = cache.get(name);
            if (ranges != null && !ranges.isEmpty()) {
                if (ranges.get(0).getFrom().compareTo(to) > 0 || ranges.get(ranges.size() - 1).getTo().compareTo(from) < 0) {
                    // the entire range [from, to] is out of cache bounds

                    // e.g. if cache is : [[2,3], [4,6], [8,9]]
                    // and requested range is : [12, 15]
                    // then ranges[ranges.Count - 1].To < from
                    // so we need to get [12,15] from server and place it
                    // at the end of the cache list

                    session.incrementRequestCount();

                    rangeResult = session.getOperations().send(new GetTimeSeriesOperation(docId, name, from, to, start, pageSize), session.sessionInfo);

                    if (rangeResult == null) {
                        return null;
                    }

                    if (!session.noTracking) {
                        int index = ranges.get(0).getFrom().compareTo(to) > 0 ? 0 : ranges.size();
                        ranges.add(index, rangeResult);
                    }

                    return rangeResult.getEntries();
                }

                SessionTimeSeriesBase.CachedEntryInfo entryInfo = serveFromCacheOrGetMissingPartsFromServerAndMerge(
                        ObjectUtils.firstNonNull(from, NetISO8601Utils.MIN_DATE), ObjectUtils.firstNonNull(to, NetISO8601Utils.MAX_DATE),
                        ranges, start, pageSize);

                if (!entryInfo.servedFromCache && !session.noTracking) {
                    InMemoryDocumentSessionOperations.addToCache(name, ObjectUtils.firstNonNull(from, NetISO8601Utils.MIN_DATE), ObjectUtils.firstNonNull(to, NetISO8601Utils.MAX_DATE),
                            entryInfo.fromRangeIndex, entryInfo.toRangeIndex, ranges, cache, entryInfo.mergedValues);
                }

                return entryInfo.resultToUser;
            }
        }

        DocumentInfo document = session.documentsById.getValue(docId);
        if (document != null) {
            JsonNode metadataTimeSeriesRaw = document.getMetadata().get(Constants.Documents.Metadata.TIME_SERIES);
            if (metadataTimeSeriesRaw != null && metadataTimeSeriesRaw.isArray()) {
                ArrayNode metadataTimeSeries = (ArrayNode) metadataTimeSeriesRaw;
                String[] timeSeries = session.mapper.convertValue(metadataTimeSeries, String[].class);
                if (!ArrayUtils.contains(timeSeries, name)) {
                    // the document is loaded in the session, but the metadata says that there is no such timeseries
                    return Collections.emptyList();
                }
            }
        }

        session.incrementRequestCount();

        rangeResult = session.getOperations().send(new GetTimeSeriesOperation(docId, name, from, to, start, pageSize), session.sessionInfo);

        if (rangeResult == null) {
            return null;
        }

        if (!session.noTracking) {
            Map<String, List<TimeSeriesRangeResult>> trackingCache = session.getTimeSeriesByDocId()
                    .computeIfAbsent(docId, k -> new TreeMap<>(String::compareToIgnoreCase));

            List<TimeSeriesRangeResult> result = new ArrayList<>();
            result.add(rangeResult);
            trackingCache.put(name, result);
        }

        return rangeResult.getEntries();
    }

    private static List<TimeSeriesEntry> skipAndTrimRangeIfNeeded(Date from, Date to, TimeSeriesRangeResult fromRange,
                                                                  TimeSeriesRangeResult toRange, List<TimeSeriesEntry> values,
                                                                  int skip, int trim) {
        if (fromRange != null && fromRange.getTo().getTime() >= from.getTime()) {
            // need to skip a part of the first range
            if (toRange != null && toRange.getFrom().getTime() <= to.getTime()) {
                // also need to trim a part of the last range
                return values.stream().skip(skip).limit(values.size() - skip - trim).collect(Collectors.toList());
            }

            return values.stream().skip(skip).collect(Collectors.toList());
        }

        if (toRange != null && toRange.getFrom().getTime() <= to.getTime()) {
            // trim a part of the last range

            return values.stream().limit(values.size() - trim).collect(Collectors.toList());
        }

        return values;
    }

    private SessionTimeSeriesBase.CachedEntryInfo serveFromCacheOrGetMissingPartsFromServerAndMerge(Date from, Date to, List<TimeSeriesRangeResult> ranges,
                                                                                                        int start, int pageSize) {

        // try to find a range in cache that contains [from, to]
        // if found, chop just the relevant part from it and return to the user.

        // otherwise, try to find two ranges (fromRange, toRange),
        // such that 'fromRange' is the last occurence for which range.From <= from
        // and 'toRange' is the first occurence for which range.To >= to.
        // At the same time, figure out the missing partial ranges that we need to get from the server.

        int toRangeIndex;
        int fromRangeIndex = -1;

        List<TimeSeriesRange> rangesToGetFromServer = null;
        List<TimeSeriesEntry> resultToUser;

        for (toRangeIndex = 0; toRangeIndex < ranges.size(); toRangeIndex++) {
            if (ranges.get(toRangeIndex).getFrom().getTime() <= from.getTime()) {
                if (ranges.get(toRangeIndex).getTo().getTime() >= to.getTime()) {
                    // we have the entire range in cache

                    resultToUser = chopRelevantRange(ranges.get(toRangeIndex), from, to);
                    return new SessionTimeSeriesBase.CachedEntryInfo(true, resultToUser, null, -1, -1);
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

            Date fromToUse = toRangeIndex == 0 || ranges.get(toRangeIndex - 1).getTo().getTime() < from.getTime()
                    ? from
                    : ranges.get(toRangeIndex - 1).getTo();
            Date toToUse = ranges.get(toRangeIndex).getFrom().getTime() <= to.getTime()
                    ? ranges.get(toRangeIndex).getFrom()
                    : to;

            rangesToGetFromServer.add(new TimeSeriesRange(name, fromToUse, toToUse));

            if (ranges.get(toRangeIndex).getTo().getTime() >= to.getTime()) {
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

        TimeSeriesDetails details = session.getOperations().send(new GetMultipleTimeSeriesOperation(docId, rangesToGetFromServer, start, pageSize), session.sessionInfo);

        // merge all the missing parts we got from server
        // with all the ranges in cache that are between 'fromRange' and 'toRange'

        Reference<List<TimeSeriesEntry>> resultToUserRef = new Reference<>();
        List<TimeSeriesEntry> mergedValues = mergeRangesWithResults(from, to, ranges, fromRangeIndex, toRangeIndex, details.getValues().get(name), resultToUserRef);
        resultToUser = resultToUserRef.value;

        return new SessionTimeSeriesBase.CachedEntryInfo(false, resultToUser, mergedValues, fromRangeIndex, toRangeIndex);
    }

    private static List<TimeSeriesEntry> mergeRangesWithResults(Date from, Date to, List<TimeSeriesRangeResult> ranges,
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
                if (ranges.get(i).getFrom().getTime() <= from.getTime() && from.getTime() <= ranges.get(i).getTo().getTime()) {
                    // requested range [from, to] starts inside 'fromRange'
                    // i.e fromRange.From <= from <= fromRange.To
                    // so we might need to skip a part of it when we return the
                    // result to the user (i.e. skip [fromRange.From, from])

                    if (ranges.get(i).getEntries() != null) {
                        for (TimeSeriesEntry v : ranges.get(i).getEntries()) {
                            mergedValues.add(v);
                            if (v.getTimestamp().getTime() < from.getTime()) {
                                skip++;
                            }

                        }
                    }
                }

                continue;
            }

            if (currentResultIndex < resultFromServer.size()
                    && resultFromServer.get(currentResultIndex).getFrom().getTime() < ranges.get(i).getFrom().getTime()) {
                // add current result from server to the merged list
                // in order to avoid duplication, skip first item in range
                // (unless this is the first time we're adding to the merged list)
                List<TimeSeriesEntry> toAdd = resultFromServer.get(currentResultIndex++)
                        .getEntries()
                        .stream()
                        .skip(mergedValues.size() == 0 ? 0 : 1)
                        .collect(Collectors.toList());
                mergedValues.addAll(toAdd);
            }

            if (i == toRangeIndex) {
                if (ranges.get(i).getFrom().getTime() <= to.getTime()) {
                    // requested range [from, to] ends inside 'toRange'
                    // so we might need to trim a part of it when we return the
                    // result to the user (i.e. trim [to, toRange.to])

                    for (int index = mergedValues.size() == 0 ? 0 : 1; index < ranges.get(i).getEntries().size(); index++) {
                        mergedValues.add(ranges.get(i).getEntries().get(index));
                        if (ranges.get(i).getEntries().get(index).getTimestamp().getTime() > to.getTime()) {
                            trim++;
                        }
                    }
                }

                continue;
            }

            // add current range from cache to the merged list.
            // in order to avoid duplication, skip first item in range if needed
            List<TimeSeriesEntry> toAdd = ranges.get(i)
                    .getEntries()
                    .stream()
                    .skip(mergedValues.size() == 0 ? 0 : 1)
                    .collect(Collectors.toList());

            mergedValues.addAll(toAdd);
        }

        if (currentResultIndex < resultFromServer.size()) {
            // the requested range ends after all the ranges in cache,
            // so the last missing part is from server
            // add last missing part to the merged list

            List<TimeSeriesEntry> toAdd = resultFromServer.get(currentResultIndex++)
                    .getEntries()
                    .stream()
                    .skip(mergedValues.size() == 0 ? 0 : 1)
                    .collect(Collectors.toList());
            mergedValues.addAll(toAdd);
        }

        resultToUserRef.value = skipAndTrimRangeIfNeeded(from, to,
                fromRangeIndex == -1 ? null : ranges.get(fromRangeIndex),
                toRangeIndex == ranges.size() ? null : ranges.get(toRangeIndex),
                mergedValues, skip, trim);

        return mergedValues;
    }

    private static List<TimeSeriesEntry> chopRelevantRange(TimeSeriesRangeResult range, Date from, Date to) {
        if (range.getEntries() == null) {
            return Collections.emptyList();
        }

        List<TimeSeriesEntry> result = new ArrayList<>();
        for (TimeSeriesEntry value : range.getEntries()) {
            if (value.getTimestamp().getTime() > to.getTime()) {
                break;
            }
            if (value.getTimestamp().getTime() < from.getTime()) {
                continue;
            }
            result.add(value);
        }

        return result;
    }

    private static class CachedEntryInfo {
        public boolean servedFromCache;
        public List<TimeSeriesEntry> resultToUser;
        public List<TimeSeriesEntry> mergedValues;
        public int fromRangeIndex;
        public int toRangeIndex;

        public CachedEntryInfo(boolean servedFromCache, List<TimeSeriesEntry> resultToUser, List<TimeSeriesEntry> mergedValues, int fromRangeIndex, int toRangeIndex) {
            this.servedFromCache = servedFromCache;
            this.resultToUser = resultToUser;
            this.mergedValues = mergedValues;
            this.fromRangeIndex = fromRangeIndex;
            this.toRangeIndex = toRangeIndex;
        }
    }
}
