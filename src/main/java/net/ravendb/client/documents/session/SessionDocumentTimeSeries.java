package net.ravendb.client.documents.session;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import net.ravendb.client.Constants;
import net.ravendb.client.documents.operations.timeSeries.*;
import net.ravendb.client.documents.session.timeSeries.TimeSeriesEntry;
import net.ravendb.client.primitives.NetISO8601Utils;
import net.ravendb.client.primitives.Reference;
import org.apache.commons.lang3.ObjectUtils;

import java.util.*;
import java.util.stream.Collectors;

public class SessionDocumentTimeSeries extends SessionTimeSeriesBase implements ISessionDocumentTimeSeries {

    public SessionDocumentTimeSeries(InMemoryDocumentSessionOperations session, String documentId, String name) {
        super(session, documentId, name);
    }

    public SessionDocumentTimeSeries(InMemoryDocumentSessionOperations session, Object entity, String name) {
        super(session, entity, name);
    }

    @Override
    public List<TimeSeriesEntry> get() {
        return get(null, null, 0, Integer.MAX_VALUE);
    }

    @Override
    public List<TimeSeriesEntry> get(Date from, Date to) {
        return get(from, to, 0, Integer.MAX_VALUE);
    }

    @Override
    public List<TimeSeriesEntry> get(Date from, Date to, int start) {
        return get(from, to, start, Integer.MAX_VALUE);
    }

    @Override
    public List<TimeSeriesEntry> get(Date from, Date to, int start, int pageSize) {
        TimeSeriesRangeResult rangeResult;
        //TODO: ??
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

                CachedEntryInfo entryInfo = serveFromCacheOrGetMissingPartsFromServerAndMerge(
                        ObjectUtils.firstNonNull(from, NetISO8601Utils.MIN_DATE), ObjectUtils.firstNonNull(to, NetISO8601Utils.MAX_DATE),
                        ranges, start, pageSize);

                if (!entryInfo.servedFromCache && !session.noTracking) {
                    addToCache(name, ObjectUtils.firstNonNull(from, NetISO8601Utils.MIN_DATE), ObjectUtils.firstNonNull(to, NetISO8601Utils.MAX_DATE),
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
                //TODO: test me!!!
                if (metadataTimeSeries.findValues(name).isEmpty()) {
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

    private CachedEntryInfo serveFromCacheOrGetMissingPartsFromServerAndMerge(Date from, Date to, List<TimeSeriesRangeResult> ranges,
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
                    return new CachedEntryInfo(true, resultToUser, null, -1, -1);
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

        return new CachedEntryInfo(false, resultToUser, mergedValues, fromRangeIndex, toRangeIndex);
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

    static void addToCache(String timeseries, Date from, Date to, int fromRangeIndex, int toRangeIndex,
                                   List<TimeSeriesRangeResult> ranges, Map<String, List<TimeSeriesRangeResult>> cache,
                                   List<TimeSeriesEntry> values) {
        if (fromRangeIndex == -1) {
            // didn't find a 'fromRange' => all ranges in cache start after 'from'

            if (toRangeIndex == ranges.size()) {
                // the requested range [from, to] contains all the ranges that are in cache

                // e.g. if cache is : [[2,3], [4,5], [7, 10]]
                // and the requested range is : [1, 15]
                // after this action cache will be : [[1, 15]]

                TimeSeriesRangeResult timeSeriesRangeResult = new TimeSeriesRangeResult();
                timeSeriesRangeResult.setFrom(from);
                timeSeriesRangeResult.setTo(to);
                timeSeriesRangeResult.setEntries(values);

                List<TimeSeriesRangeResult> result = new ArrayList<>();
                result.add(timeSeriesRangeResult);
                cache.put(timeseries, result);

                return;
            }

            if (ranges.get(toRangeIndex).getFrom().getTime() > to.getTime()) {
                // requested range ends before 'toRange' starts
                // remove all ranges that come before 'toRange' from cache
                // add the new range at the beginning of the list

                // e.g. if cache is : [[2,3], [4,5], [7,10]]
                // and the requested range is : [1,6]
                // after this action cache will be : [[1,6], [7,10]]

                ranges.subList(0, toRangeIndex).clear();
                TimeSeriesRangeResult timeSeriesRangeResult = new TimeSeriesRangeResult();
                timeSeriesRangeResult.setFrom(from);
                timeSeriesRangeResult.setTo(to);
                timeSeriesRangeResult.setEntries(values);

                ranges.add(0, timeSeriesRangeResult);

                return;
            }

            // the requested range ends inside 'toRange'
            // merge the result from server into 'toRange'
            // remove all ranges that come before 'toRange' from cache

            // e.g. if cache is : [[2,3], [4,5], [7,10]]
            // and the requested range is : [1,8]
            // after this action cache will be : [[1,10]]

            ranges.get(toRangeIndex).setFrom(from);
            ranges.get(toRangeIndex).setEntries(values);
            ranges.subList(0, toRangeIndex).clear();

            return;
        }

        // found a 'fromRange'

        if (toRangeIndex == ranges.size()) {
            // didn't find a 'toRange' => all the ranges in cache end before 'to'

            if (ranges.get(fromRangeIndex).getTo().getTime() < from.getTime()) {
                // requested range starts after 'fromRange' ends,
                // so it needs to be placed right after it
                // remove all the ranges that come after 'fromRange' from cache
                // add the merged values as a new range at the end of the list

                // e.g. if cache is : [[2,3], [5,6], [7,10]]
                // and the requested range is : [4,12]
                // then 'fromRange' is : [2,3]
                // after this action cache will be : [[2,3], [4,12]]


                ranges.subList(fromRangeIndex + 1, ranges.size()).clear();
                TimeSeriesRangeResult timeSeriesRangeResult = new TimeSeriesRangeResult();
                timeSeriesRangeResult.setFrom(from);
                timeSeriesRangeResult.setTo(to);
                timeSeriesRangeResult.setEntries(values);

                ranges.add(timeSeriesRangeResult);

                return;
            }

            // the requested range starts inside 'fromRange'
            // merge result into 'fromRange'
            // remove all the ranges from cache that come after 'fromRange'

            // e.g. if cache is : [[2,3], [4,6], [7,10]]
            // and the requested range is : [5,12]
            // then 'fromRange' is [4,6]
            // after this action cache will be : [[2,3], [4,12]]

            ranges.get(fromRangeIndex).setTo(to);
            ranges.get(fromRangeIndex).setEntries(values);
            ranges.subList(fromRangeIndex + 1, ranges.size()).clear();

            return;
        }

        // found both 'fromRange' and 'toRange'
        // the requested range is inside cache bounds

        if (ranges.get(fromRangeIndex).getTo().getTime() < from.getTime()) {
            // requested range starts after 'fromRange' ends

            if (ranges.get(toRangeIndex).getFrom().getTime() > to.getTime())
            {
                // requested range ends before 'toRange' starts

                // remove all ranges in between 'fromRange' and 'toRange'
                // place new range in between 'fromRange' and 'toRange'

                // e.g. if cache is : [[2,3], [5,6], [7,8], [10,12]]
                // and the requested range is : [4,9]
                // then 'fromRange' is [2,3] and 'toRange' is [10,12]
                // after this action cache will be : [[2,3], [4,9], [10,12]]

                ranges.subList(fromRangeIndex + 1, toRangeIndex).clear();

                TimeSeriesRangeResult timeSeriesRangeResult = new TimeSeriesRangeResult();
                timeSeriesRangeResult.setFrom(from);
                timeSeriesRangeResult.setTo(to);
                timeSeriesRangeResult.setEntries(values);

                ranges.add(fromRangeIndex + 1, timeSeriesRangeResult);

                return;
            }

            // requested range ends inside 'toRange'

            // merge the new range into 'toRange'
            // remove all ranges in between 'fromRange' and 'toRange'

            // e.g. if cache is : [[2,3], [5,6], [7,10]]
            // and the requested range is : [4,9]
            // then 'fromRange' is [2,3] and 'toRange' is [7,10]
            // after this action cache will be : [[2,3], [4,10]]

            ranges.subList(fromRangeIndex + 1, toRangeIndex).clear();
            ranges.get(toRangeIndex).setFrom(from);
            ranges.get(toRangeIndex).setEntries(values);

            return;
        }

        // the requested range starts inside 'fromRange'

        if (ranges.get(toRangeIndex).getFrom().getTime() > to.getTime())
        {
            // requested range ends before 'toRange' starts

            // remove all ranges in between 'fromRange' and 'toRange'
            // merge new range into 'fromRange'

            // e.g. if cache is : [[2,4], [5,6], [8,10]]
            // and the requested range is : [3,7]
            // then 'fromRange' is [2,4] and 'toRange' is [8,10]
            // after this action cache will be : [[2,7], [8,10]]

            ranges.get(fromRangeIndex).setTo(to);
            ranges.get(fromRangeIndex).setEntries(values);
            ranges.subList(fromRangeIndex + 1, toRangeIndex).clear();

            return;
        }

        // the requested range starts inside 'fromRange'
        // and ends inside 'toRange'

        // merge all ranges in between 'fromRange' and 'toRange'
        // into a single range [fromRange.From, toRange.To]

        // e.g. if cache is : [[2,4], [5,6], [8,10]]
        // and the requested range is : [3,9]
        // then 'fromRange' is [2,4] and 'toRange' is [8,10]
        // after this action cache will be : [[2,10]]

        ranges.get(fromRangeIndex).setTo(ranges.get(toRangeIndex).getTo());
        ranges.get(fromRangeIndex).setEntries(values);
        ranges.subList(fromRangeIndex + 1, toRangeIndex + 1).clear();
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
