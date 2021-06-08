package net.ravendb.client.documents.operations.analyzers;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.node.ObjectNode;
import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.indexes.analysis.AnalyzerDefinition;
import net.ravendb.client.documents.operations.IVoidMaintenanceOperation;
import net.ravendb.client.http.IRaftCommand;
import net.ravendb.client.http.ServerNode;
import net.ravendb.client.http.VoidRavenCommand;
import net.ravendb.client.json.ContentProviderHttpEntity;
import net.ravendb.client.primitives.Reference;
import net.ravendb.client.util.RaftIdGenerator;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.ContentType;

import java.io.IOException;

public class PutAnalyzersOperation implements IVoidMaintenanceOperation {
    private final AnalyzerDefinition[] _analyzersToAdd;

    public PutAnalyzersOperation(AnalyzerDefinition... analyzersToAdd) {
        if (analyzersToAdd == null || analyzersToAdd.length == 0) {
            throw new IllegalArgumentException("AnalyzersToAdd cannot be null or empty");
        }

        _analyzersToAdd = analyzersToAdd;
    }

    @Override
    public VoidRavenCommand getCommand(DocumentConventions conventions) {
        return new PutAnalyzersCommand(conventions, _analyzersToAdd);
    }

    private static class PutAnalyzersCommand extends VoidRavenCommand implements IRaftCommand {
        private final ObjectNode[] _analyzersToAdd;

        public PutAnalyzersCommand(DocumentConventions conventions, AnalyzerDefinition[] analyzersToAdd) {
            if (conventions == null) {
                throw new IllegalArgumentException("Conventions cannot be null");
            }
            if (analyzersToAdd == null) {
                throw new IllegalArgumentException("AnalyzersToAdd cannot be null");
            }

            _analyzersToAdd = new ObjectNode[analyzersToAdd.length];

            for (int i = 0; i < analyzersToAdd.length; i++) {
                if (analyzersToAdd[i].getName() == null) {
                    throw new IllegalArgumentException("Name cannot be null");
                }

                _analyzersToAdd[i] = mapper.valueToTree(analyzersToAdd[i]);
            }
        }

        @Override
        public HttpRequestBase createRequest(ServerNode node, Reference<String> url) {
            url.value = node.getUrl() + "/databases/" + node.getDatabase() + "/admin/analyzers";

            HttpPut request = new HttpPut();
            request.setEntity(new ContentProviderHttpEntity(outputStream -> {
                try (JsonGenerator generator = mapper.getFactory().createGenerator(outputStream)) {
                    generator.writeStartObject();
                    generator.writeFieldName("Analyzers");
                    generator.writeStartArray();
                    for (ObjectNode analyzer : _analyzersToAdd) {
                        generator.writeObject(analyzer);
                    }
                    generator.writeEndArray();
                    generator.writeEndObject();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }, ContentType.APPLICATION_JSON));

            return request;
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
