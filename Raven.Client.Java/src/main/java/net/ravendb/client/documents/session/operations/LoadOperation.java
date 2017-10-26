package net.ravendb.client.documents.session.operations;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Defaults;
import net.ravendb.client.documents.commands.GetDocumentCommand;
import net.ravendb.client.documents.commands.GetDocumentResult;
import net.ravendb.client.documents.session.DocumentInfo;
import net.ravendb.client.documents.session.InMemoryDocumentSessionOperations;
import net.ravendb.client.http.RequestExecutor;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.ArrayList;
import java.util.List;

public class LoadOperation {

    private final InMemoryDocumentSessionOperations _session;
    private static final Log logger = LogFactory.getLog(LoadOperation.class);

    private String[] _ids;
    private String[] _includes;
    private final List<String> _idsToCheckOnServer = new ArrayList<>();

    public LoadOperation(InMemoryDocumentSessionOperations _session) {
        this._session = _session;
    }

    public GetDocumentCommand createRequest() {
        if (_idsToCheckOnServer.size() == 0) {
            return null;
        }

        /* TODO:
        if (_session.CheckIfIdAlreadyIncluded(_ids, _includes))
                return null;
         */

        _session.incrementRequestCount();

        if (logger.isInfoEnabled()) {
            logger.info("Requesting the following ids " + String.join(",", _idsToCheckOnServer) + " from " + _session.storeIdentifier());
        }

        return new GetDocumentCommand(_idsToCheckOnServer.toArray(new String[0]), _includes, false);
    }

    public LoadOperation byId(String id) {
        if (id == null) {
            return this;
        }

        if (_ids == null) {
            _ids = new String[] { id };
        }

        if (_session.isLoadedOrDeleted(id)) {
            return this;
        }

        _idsToCheckOnServer.add(id);
        return this;
    }

    /* TODO:


        public LoadOperation WithIncludes(string[] includes)
        {
            _includes = includes;
            return this;
        }

        public LoadOperation ByIds(IEnumerable<string> ids)
        {
            _ids = ids.ToArray();
            foreach (var id in _ids.Distinct(StringComparer.OrdinalIgnoreCase))
            {
                ById(id);
            }

            return this;
        }
*/

    public <T> T getDocument(Class<T> clazz) {
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

        /* TODO
         if (_session.IncludedDocumentsById.TryGetValue(id, out doc))
                return _session.TrackEntity<T>(doc);
         */

        return Defaults.defaultValue(clazz);
    }

    /* TODO:


        public Dictionary<string, T> GetDocuments<T>()
        {
            var finalResults = new Dictionary<string, T>(StringComparer.OrdinalIgnoreCase);
            for (var i = 0; i < _ids.Length; i++)
            {
                var id = _ids[i];
                if (id == null)
                    continue;
                finalResults[id] = GetDocument<T>(id);
            }
            return finalResults;
        }
*/

    public void setResult(GetDocumentResult result) {
        if (result == null) {
            return;
        }

        // TODO: _session.RegisterIncludes(result.Includes);


        for (JsonNode document : result.getResults()) {
            if (document == null) {
                continue;
            }

            DocumentInfo newDocumentInfo = DocumentInfo.getNewDocumentInfo((ObjectNode) document);
            _session.documentsById.add(newDocumentInfo);
        }
        //TODO: _session.RegisterMissingIncludes(result.Results, result.Includes, _includes);
    }
}
