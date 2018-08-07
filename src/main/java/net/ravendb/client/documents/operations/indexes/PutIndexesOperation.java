package net.ravendb.client.documents.operations.indexes;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.indexes.IndexDefinition;
import net.ravendb.client.documents.indexes.IndexTypeExtensions;
import net.ravendb.client.documents.indexes.PutIndexResult;
import net.ravendb.client.documents.operations.IMaintenanceOperation;
import net.ravendb.client.documents.operations.PutIndexesResponse;
import net.ravendb.client.extensions.JsonExtensions;
import net.ravendb.client.http.RavenCommand;
import net.ravendb.client.http.ServerNode;
import net.ravendb.client.json.ContentProviderHttpEntity;
import net.ravendb.client.primitives.Reference;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.ContentType;

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

    private class PutIndexesCommand extends RavenCommand<PutIndexResult[]> {
        private final ObjectNode[] _indexToAdd;

        public PutIndexesCommand(DocumentConventions conventions, IndexDefinition[] indexesToAdd) {
            super(PutIndexResult[].class);

            if (conventions == null) {
                throw new IllegalArgumentException("conventions cannot be null");
            }

            if (indexesToAdd == null) {
                throw new IllegalArgumentException("indexesToAdd cannot be null");
            }

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
        public HttpRequestBase createRequest(ServerNode node, Reference<String> url) {
            url.value = node.getUrl() + "/databases/" + node.getDatabase() + (_allJavaScriptIndexes ? "/indexes" : "/admin/indexes");

            HttpPut httpPut = new HttpPut();

            ObjectMapper mapper = JsonExtensions.getDefaultMapper();

            httpPut.setEntity(new ContentProviderHttpEntity(outputStream -> {
                try (JsonGenerator generator = mapper.getFactory().createGenerator(outputStream)) {
                    generator.writeStartObject();
                    generator.writeFieldName("Indexes");
                    generator.writeStartArray();

                    for (ObjectNode jsonNodes : _indexToAdd) {
                        generator.writeObject(jsonNodes);
                    }

                    generator.writeEndArray();
                    generator.writeEndObject();

                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }, ContentType.APPLICATION_JSON));
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
    }

}
