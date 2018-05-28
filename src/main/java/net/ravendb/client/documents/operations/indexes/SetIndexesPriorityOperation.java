package net.ravendb.client.documents.operations.indexes;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.node.ObjectNode;
import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.indexes.IndexPriority;
import net.ravendb.client.documents.operations.IVoidMaintenanceOperation;
import net.ravendb.client.http.ServerNode;
import net.ravendb.client.http.VoidRavenCommand;
import net.ravendb.client.json.ContentProviderHttpEntity;
import net.ravendb.client.primitives.Reference;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.ContentType;

import java.io.IOException;

public class SetIndexesPriorityOperation implements IVoidMaintenanceOperation {

    private final Parameters _parameters;

    public SetIndexesPriorityOperation(String indexName, IndexPriority priority) {
        if (indexName == null) {
            throw new IllegalArgumentException("IndexName cannot be null");
        }

        _parameters = new Parameters();
        _parameters.setPriority(priority);
        _parameters.setIndexNames(new String[]{ indexName });
    }

    public SetIndexesPriorityOperation(Parameters parameters) {
        if (parameters == null) {
            throw new IllegalArgumentException("Parameters cannot be null");
        }

        if (parameters.getIndexNames() == null || parameters.getIndexNames().length == 0) {
            throw new IllegalArgumentException("IndexNames cannot be null or empty");
        }

        _parameters = parameters;
    }

    @Override
    public VoidRavenCommand getCommand(DocumentConventions conventions) {
        return new SetIndexesPriorityOperation.SetIndexPriorityCommand(conventions, _parameters);
    }

    private static class SetIndexPriorityCommand extends VoidRavenCommand {
        private final ObjectNode _parameters;

        public SetIndexPriorityCommand(DocumentConventions conventions, Parameters parameters) {
            if (conventions == null) {
                throw new IllegalArgumentException("Conventions cannot be null");
            }
            if (parameters == null) {
                throw new IllegalArgumentException("Parameters cannot be null");
            }

            _parameters = mapper.valueToTree(parameters);
        }

        @Override
        public HttpRequestBase createRequest(ServerNode node, Reference<String> url) {
            url.value = node.getUrl() + "/databases/" + node.getDatabase() + "/indexes/set-priority";

            HttpPost request = new HttpPost();

            request.setEntity(new ContentProviderHttpEntity(outputStream -> {
                try (JsonGenerator generator = mapper.getFactory().createGenerator(outputStream)) {
                    generator.writeTree(_parameters);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }, ContentType.APPLICATION_JSON));

            return request;
        }
    }

    public static class Parameters {
        private String[] indexNames;
        private IndexPriority priority;

        public String[] getIndexNames() {
            return indexNames;
        }

        public void setIndexNames(String[] indexNames) {
            this.indexNames = indexNames;
        }

        public IndexPriority getPriority() {
            return priority;
        }

        public void setPriority(IndexPriority priority) {
            this.priority = priority;
        }
    }
}
