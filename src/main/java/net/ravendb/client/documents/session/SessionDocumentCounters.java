package net.ravendb.client.documents.session;

import com.fasterxml.jackson.databind.JsonNode;
import net.ravendb.client.Constants;
import net.ravendb.client.documents.operations.counters.CounterDetail;
import net.ravendb.client.documents.operations.counters.CountersDetail;
import net.ravendb.client.documents.operations.counters.GetCountersOperation;
import net.ravendb.client.primitives.Tuple;

import java.util.*;

public class SessionDocumentCounters extends SessionCountersBase implements ISessionDocumentCounters {

    public SessionDocumentCounters(InMemoryDocumentSessionOperations session, String documentId) {
        super(session, documentId);
    }

    public SessionDocumentCounters(InMemoryDocumentSessionOperations session, Object entity) {
        super(session, entity);
    }

    @Override
    public Map<String, Long> getAll() {
        Tuple<Boolean, Map<String, Long>> cache = session.getCountersByDocId().get(docId);

        if (cache == null) {
            cache = Tuple.create(false, new TreeMap<>(String::compareToIgnoreCase));
        }

        boolean missingCounters = !cache.first;

        DocumentInfo document = session.documentsById.getValue(docId);
        if (document != null) {
            JsonNode metadataCounters = document.getMetadata().get(Constants.Documents.Metadata.COUNTERS);
            if (metadataCounters == null || metadataCounters.isNull()) {
                missingCounters = false;
            } else if (cache.second.size() >= metadataCounters.size()) {
                missingCounters = false;

                for (JsonNode c : metadataCounters) {
                    if (cache.second.containsKey(c.textValue())) {
                        continue;
                    }
                    missingCounters = true;
                    break;
                }
            }
        }

        if (missingCounters) {
            // we either don't have the document in session and GotAll = false,
            // or we do and cache doesn't contain all metadata counters

            session.incrementRequestCount();

            CountersDetail details = session.getOperations().send(new GetCountersOperation(docId), session.sessionInfo);
            cache.second.clear();

            for (CounterDetail counterDetail : details.getCounters()) {
                cache.second.put(counterDetail.getCounterName(), counterDetail.getTotalValue());
            }
        }

        cache.first = true;

        if (!session.noTracking) {
            session.getCountersByDocId().put(docId, cache);
        }

        return cache.second;
    }

    @Override
    public Long get(String counter) {
        Long value = null;

        Tuple<Boolean, Map<String, Long>> cache = session.getCountersByDocId().get(docId);
        if (cache != null) {
            value = cache.second.get(counter);
            if (cache.second.containsKey(counter)) {
                return value;
            }
        } else {
            cache = Tuple.create(false, new TreeMap<>(String::compareToIgnoreCase));
        }

        DocumentInfo document = session.documentsById.getValue(docId);
        boolean metadataHasCounterName = false;
        if (document != null) {
            JsonNode metadataCounters = document.getMetadata().get(Constants.Documents.Metadata.COUNTERS);
            if (metadataCounters != null && !metadataCounters.isNull()) {
                for (JsonNode node : metadataCounters) {
                    if (node.asText().equalsIgnoreCase(counter)) {
                        metadataHasCounterName = true;
                    }
                }
            }
        }
        if ((document == null && !cache.first) || metadataHasCounterName) {
            // we either don't have the document in session and GotAll = false,
            // or we do and it's metadata contains the counter name

            session.incrementRequestCount();

            CountersDetail details = session.getOperations().send(new GetCountersOperation(docId, counter), session.sessionInfo);
            if (details.getCounters() != null && !details.getCounters().isEmpty()) {
                value = details.getCounters().get(0).getTotalValue();
            }
        }

        cache.second.put(counter, value);

        if (!session.noTracking) {
            session.getCountersByDocId().put(docId, cache);
        }

        return value;
    }

    @Override
    public Map<String, Long> get(Collection<String> counters) {
        Tuple<Boolean, Map<String, Long>> cache = session.getCountersByDocId().get(docId);
        if (cache == null) {
            cache = Tuple.create(false, new TreeMap<>(String::compareToIgnoreCase));
        }

        JsonNode metadataCounters = null;
        DocumentInfo document = session.documentsById.getValue(docId);
        if (document != null) {
            metadataCounters = document.getMetadata().get(Constants.Documents.Metadata.COUNTERS);
        }

        Map<String, Long> result = new HashMap<>();

        for (String counter : counters) {
            boolean hasCounter = cache.second.containsKey(counter);
            Long val = cache.second.get(counter);
            boolean notInMetadata = true;

            if (document != null && metadataCounters != null) {
                for (JsonNode metadataCounter : metadataCounters) {
                    if (metadataCounter.asText().equalsIgnoreCase(counter)) {
                        notInMetadata = false;
                    }
                }
            }
            if (hasCounter || cache.first || (document != null && notInMetadata)) {
                // we either have value in cache,
                // or we have the metadata and the counter is not there,
                // or GotAll

                result.put(counter, val);
                continue;
            }

            result.clear();

            session.incrementRequestCount();

            CountersDetail details = session.getOperations().send(new GetCountersOperation(docId, counters.toArray(new String[0])), session.sessionInfo);

            for (CounterDetail counterDetail : details.getCounters()) {
                cache.second.put(counterDetail.getCounterName(), counterDetail.getTotalValue());
                result.put(counterDetail.getCounterName(), counterDetail.getTotalValue());
            }

            break;
        }

        if (!session.noTracking) {
            session.getCountersByDocId().put(docId, cache);
        }

        return result;
    }
}
