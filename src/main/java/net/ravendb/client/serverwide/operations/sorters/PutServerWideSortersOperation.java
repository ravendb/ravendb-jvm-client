package net.ravendb.client.serverwide.operations.sorters;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.node.ObjectNode;
import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.queries.sorting.SorterDefinition;
import net.ravendb.client.http.IRaftCommand;
import net.ravendb.client.http.ServerNode;
import net.ravendb.client.http.VoidRavenCommand;
import net.ravendb.client.json.ContentProviderHttpEntity;
import net.ravendb.client.serverwide.operations.IVoidServerOperation;
import net.ravendb.client.util.RaftIdGenerator;
import org.apache.hc.client5.http.classic.methods.HttpPut;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.core5.http.ContentType;

public class PutServerWideSortersOperation implements IVoidServerOperation {
    private final SorterDefinition[] _sortersToAdd;

    public PutServerWideSortersOperation(SorterDefinition... sortersToAdd) {
        if (sortersToAdd == null || sortersToAdd.length == 0) {
            throw new IllegalArgumentException("SortersToAdd cannot be null or empty");
        }

        _sortersToAdd = sortersToAdd;
    }

    @Override
    public VoidRavenCommand getCommand(DocumentConventions conventions) {
        return new PutServerWideSortersCommand(conventions, _sortersToAdd);
    }

    private static class PutServerWideSortersCommand extends VoidRavenCommand implements IRaftCommand {
        private final ObjectNode[] _sortersToAdd;
        private final DocumentConventions _conventions;

        public PutServerWideSortersCommand(DocumentConventions conventions, SorterDefinition[] sortersToAdd) {
            if (conventions == null) {
                throw new IllegalArgumentException("Conventions cannot be null");
            }

            if (sortersToAdd == null) {
                throw new IllegalArgumentException("SortersToAdd cannot be null");
            }

            _conventions = conventions;
            _sortersToAdd = new ObjectNode[sortersToAdd.length];

            for (int i = 0; i < sortersToAdd.length; i++) {
                if (sortersToAdd[i].getName() == null) {
                    throw new IllegalArgumentException("Sorter name cannot be null");
                }

                _sortersToAdd[i] = mapper.valueToTree(sortersToAdd[i]);
            }
        }

        @Override
        public HttpUriRequestBase createRequest(ServerNode node) {
            String url = node.getUrl() + "/admin/sorters";

            HttpPut request = new HttpPut(url);
            request.setEntity(new ContentProviderHttpEntity(outputStream -> {
                try (JsonGenerator generator = createSafeJsonGenerator(outputStream)) {
                    generator.writeStartObject();
                    generator.writeFieldName("Sorters");
                    generator.writeStartArray();
                    for (ObjectNode sorter : _sortersToAdd) {
                        generator.writeObject(sorter);
                    }
                    generator.writeEndArray();
                    generator.writeEndObject();
                }
            }, ContentType.APPLICATION_JSON, _conventions));

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
