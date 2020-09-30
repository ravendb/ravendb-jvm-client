package net.ravendb.client.documents.session.operations;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Defaults;
import net.ravendb.client.documents.commands.GetDocumentsCommand;
import net.ravendb.client.documents.commands.GetDocumentsResult;
import net.ravendb.client.documents.operations.timeSeries.TimeSeriesRange;
import net.ravendb.client.documents.session.DocumentInfo;
import net.ravendb.client.documents.session.InMemoryDocumentSessionOperations;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.*;

public class LoadOperation {

    private final InMemoryDocumentSessionOperations _session;
    private static final Log logger = LogFactory.getLog(LoadOperation.class);

    private String[] _ids;
    private String[] _includes;
    private String[] _countersToInclude;
    private String[] _compareExchangeValuesToInclude;
    private boolean _includeAllCounters;
    private List<TimeSeriesRange> _timeSeriesToInclude;

    private boolean _resultsSet;
    private GetDocumentsResult _results;

    public LoadOperation(InMemoryDocumentSessionOperations _session) {
        this._session = _session;
    }

    public GetDocumentsCommand createRequest() {
        if (_session.checkIfIdAlreadyIncluded(_ids, _includes != null ? Arrays.asList(_includes) : null)) {
            return null;
        }

        _session.incrementRequestCount();

        if (logger.isInfoEnabled()) {
            logger.info("Requesting the following ids " + String.join(",", _ids) + " from " + _session.storeIdentifier());
        }

        if (_includeAllCounters) {
            return new GetDocumentsCommand(_ids, _includes, true, _timeSeriesToInclude, _compareExchangeValuesToInclude, false);
        }

        return new GetDocumentsCommand(_ids, _includes, _countersToInclude, _timeSeriesToInclude, _compareExchangeValuesToInclude, false);
    }

    public LoadOperation byId(String id) {
        if (StringUtils.isBlank(id)) {
            return this;
        }

        if (_ids == null) {
            _ids = new String[] { id };
        }

        return this;
    }

    public LoadOperation withIncludes(String[] includes) {
        _includes = includes;
        return this;
    }

    public LoadOperation withCompareExchange(String[] compareExchangeValues) {
        _compareExchangeValuesToInclude = compareExchangeValues;
        return this;
    }

    public LoadOperation withCounters(String[] counters) {
        if (counters != null) {
            _countersToInclude = counters;
        }
        return this;
    }

    public LoadOperation withAllCounters() {
        _includeAllCounters = true;
        return this;
    }

    public LoadOperation withTimeSeries(List<TimeSeriesRange> timeSeries) {
        if (timeSeries != null) {
            _timeSeriesToInclude = timeSeries;
        }
        return this;
    }

    public LoadOperation byIds(String[] ids) {
        return byIds(Arrays.asList(ids));
    }

    public LoadOperation byIds(Collection<String> ids) {
        Set<String> distinct = new TreeSet<>(String::compareToIgnoreCase);

        for (String id : ids) {
            if (!StringUtils.isBlank(id)) {
                distinct.add(id);
            }
        }

        _ids = distinct.toArray(new String[0]);

        return this;
    }

    public <T> T getDocument(Class<T> clazz) {
        if (_session.noTracking) {
            if (!_resultsSet && _ids.length > 0) {
                throw new IllegalStateException("Cannot execute getDocument before operation execution.");
            }

            if (_results == null || _results.getResults() == null || _results.getResults().size() == 0) {
                return null;
            }

            ObjectNode document = (ObjectNode) _results.getResults().get(0);
            if (document == null) {
                return null;
            }

            DocumentInfo documentInfo = DocumentInfo.getNewDocumentInfo(document);
            return _session.trackEntity(clazz, documentInfo);
        }

        return getDocument(clazz, _ids[0]);
    }

    private <T> T getDocument(Class<T> clazz, String id) {
        if (id == null) {
            return Defaults.defaultValue(clazz);
        }

        if (_session.isDeleted(id)) {
            return Defaults.defaultValue(clazz);
        }

        DocumentInfo doc = _session.documentsById.getValue(id);
        if (doc != null) {
            return _session.trackEntity(clazz, doc);
        }


        doc = _session.includedDocumentsById.get(id);
        if (doc != null) {
            return _session.trackEntity(clazz, doc);
        }

        return Defaults.defaultValue(clazz);
    }

    public <T> Map<String, T> getDocuments(Class<T> clazz) {
        Map<String, T> finalResults = new TreeMap<>(String::compareToIgnoreCase);

        if (_session.noTracking) {
            if (!_resultsSet && _ids.length > 0) {
                throw new IllegalStateException("Cannot execute 'getDocuments' before operation execution.");
            }

            for (String id : _ids) {
                if (id == null) {
                    continue;
                }

                finalResults.put(id, null);
            }

            if (_results == null || _results.getResults() == null || _results.getResults().size() == 0) {
                return finalResults;
            }

            for (JsonNode document : _results.getResults()) {
                if (document == null || document.isNull()) {
                    continue;
                }

                DocumentInfo newDocumentInfo = DocumentInfo.getNewDocumentInfo((ObjectNode) document);
                finalResults.put(newDocumentInfo.getId(), _session.trackEntity(clazz, newDocumentInfo));
            }

            return finalResults;
        }

        for (String id : _ids) {
            if (id == null) {
                continue;
            }

            finalResults.put(id, getDocument(clazz, id));
        }

        return finalResults;

    }
    public void setResult(GetDocumentsResult result) {
        _resultsSet = true;

        if (_session.noTracking) {
            _results = result;
            return;
        }

        if (result == null) {
            _session.registerMissing(_ids);
            return;
        }

        _session.registerIncludes(result.getIncludes());

        if (_includeAllCounters || _countersToInclude != null) {
            _session.registerCounters(result.getCounterIncludes(), _ids, _countersToInclude, _includeAllCounters);
        }

        if (_timeSeriesToInclude != null) {
            _session.registerTimeSeries(result.getTimeSeriesIncludes());
        }

        if (_compareExchangeValuesToInclude != null) {
            _session.getClusterSession().registerCompareExchangeValues(result.getCompareExchangeValueIncludes());
        }

        for (JsonNode document : result.getResults()) {
            if (document == null || document.isNull()) {
                continue;
            }

            DocumentInfo newDocumentInfo = DocumentInfo.getNewDocumentInfo((ObjectNode) document);
            _session.documentsById.add(newDocumentInfo);
        }

        for (String id : _ids) {
            DocumentInfo value = _session.documentsById.getValue(id);
            if (value == null) {
                _session.registerMissing(id);
            }
        }

        _session.registerMissingIncludes(result.getResults(), result.getIncludes(), _includes);
    }
}
