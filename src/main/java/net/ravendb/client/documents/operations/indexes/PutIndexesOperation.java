package net.ravendb.client.documents.operations.indexes;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.node.ObjectNode;
import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.indexes.IndexDefinition;
import net.ravendb.client.documents.indexes.IndexTypeExtensions;
import net.ravendb.client.documents.indexes.PutIndexResult;
import net.ravendb.client.documents.operations.IMaintenanceOperation;
import net.ravendb.client.documents.operations.PutIndexesResponse;
import net.ravendb.client.http.IRaftCommand;
import net.ravendb.client.http.RavenCommand;
import net.ravendb.client.http.ServerNode;
import net.ravendb.client.json.ContentProviderHttpEntity;
import net.ravendb.client.util.RaftIdGenerator;
import org.apache.hc.client5.http.classic.methods.HttpPut;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.core5.http.ContentType;

import java.io.IOException;

public class PutIndexesOperation implements IMaintenanceOperation<PutIndexResult[]> {

    private final IndexDefinition[] _indexToAdd;
    private boolean _allJavaScriptIndexes;

    public PutIndexesOperation(IndexDefinition... indexToAdd) {
        if (indexToAdd == null || indexToAdd.length == 0) {
            throw new IllegalArgumentException("indexToAdd cannot be null");
        }
        this._indexToAdd = indexToAdd;
    }

    @Override
    public RavenCommand<PutIndexResult[]> getCommand(DocumentConventions conventions) {
        return new PutIndexesCommand(conventions, _indexToAdd);
    }

    private class PutIndexesCommand extends RavenCommand<PutIndexResult[]> implements IRaftCommand {
        private final ObjectNode[] _indexToAdd;
        private final DocumentConventions _conventions;

        public PutIndexesCommand(DocumentConventions conventions, IndexDefinition[] indexesToAdd) {
            super(PutIndexResult[].class);

            if (conventions == null) {
                throw new IllegalArgumentException("conventions cannot be null");
            }

            if (indexesToAdd == null) {
                throw new IllegalArgumentException("indexesToAdd cannot be null");
            }

            _conventions = conventions;
            _indexToAdd = new ObjectNode[indexesToAdd.length];
            _allJavaScriptIndexes = true;

            for (int i = 0; i < indexesToAdd.length; i++) {
                //We validate on the server that it is indeed a javascript index.

                if (!IndexTypeExtensions.isJavaScript(indexesToAdd[i].getType())) {
                    _allJavaScriptIndexes = false;
                }

                if (indexesToAdd[i].getName() == null) {
                    throw new IllegalArgumentException("Index name cannot be null");
                }
                _indexToAdd[i] = mapper.valueToTree(indexesToAdd[i]);
            }
        }

        @Override
        public HttpUriRequestBase createRequest(ServerNode node) {
            String url = node.getUrl() + "/databases/" + node.getDatabase() + (_allJavaScriptIndexes ? "/indexes" : "/admin/indexes");

            HttpPut httpPut = new HttpPut(url);

            httpPut.setEntity(new ContentProviderHttpEntity(outputStream -> {
                try (JsonGenerator generator = createSafeJsonGenerator(outputStream)) {
                    generator.writeStartObject();
                    generator.writeFieldName("Indexes");
                    generator.writeStartArray();

                    for (ObjectNode jsonNodes : _indexToAdd) {
                        generator.writeObject(jsonNodes);
                    }

                    generator.writeEndArray();
                    generator.writeEndObject();
                }
            }, ContentType.APPLICATION_JSON, _conventions));
            return httpPut;
        }

        @Override
        public void setResponse(String response, boolean fromCache) throws IOException {
            result = mapper.readValue(response, PutIndexesResponse.class).getResults();
        }

        @Override
        public boolean isReadRequest() {
            return false;
        }

        @Override
        public String getRaftUniqueRequestId() {
            return RaftIdGenerator.newId();
        }
    }

}
