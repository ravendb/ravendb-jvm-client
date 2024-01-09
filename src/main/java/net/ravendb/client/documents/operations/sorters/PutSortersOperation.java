package net.ravendb.client.documents.operations.sorters;

import com.fasterxml.jackson.core.JsonGenerator;
import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.operations.IVoidMaintenanceOperation;
import net.ravendb.client.documents.queries.sorting.SorterDefinition;
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
import java.util.Arrays;
import java.util.Objects;

public class PutSortersOperation implements IVoidMaintenanceOperation {
    private final SorterDefinition[] _sortersToAdd;

    public PutSortersOperation(SorterDefinition... sortersToAdd) {
        if (sortersToAdd == null || sortersToAdd.length == 0) {
            throw new IllegalArgumentException("SortersToAdd cannot be null or empty");
        }

        _sortersToAdd = sortersToAdd;
    }

    @Override
    public VoidRavenCommand getCommand(DocumentConventions conventions) {
        return new PutSortersCommand(conventions, _sortersToAdd);
    }

    private static class PutSortersCommand extends VoidRavenCommand implements IRaftCommand {
        private final SorterDefinition[] _sortersToAdd;
        private final DocumentConventions _conventions;

        public PutSortersCommand(DocumentConventions conventions, SorterDefinition[] sortersToAdd) {
            if (conventions == null) {
                throw new IllegalArgumentException("Conventions cannot be null");
            }

            if (sortersToAdd == null) {
                throw new IllegalArgumentException("SortersToAdd cannot be null");
            }

            if (Arrays.stream(sortersToAdd).anyMatch(Objects::isNull)) {
                throw new IllegalArgumentException("Sorter cannot be null");
            }

            _conventions = conventions;
            _sortersToAdd = sortersToAdd;
        }

        @Override
        public HttpRequestBase createRequest(ServerNode node, Reference<String> url) {
            url.value = node.getUrl() + "/databases/" + node.getDatabase() + "/admin/sorters";

            HttpPut request = new HttpPut();
            request.setEntity(new ContentProviderHttpEntity(outputStream -> {
                try (JsonGenerator generator = createSafeJsonGenerator(outputStream)) {
                    generator.writeStartObject();
                    generator.writeFieldName("Sorters");
                    generator.getCodec().writeValue(generator, _sortersToAdd);
                    generator.writeEndObject();
                } catch (IOException e) {
                    throw new RuntimeException(e);
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
