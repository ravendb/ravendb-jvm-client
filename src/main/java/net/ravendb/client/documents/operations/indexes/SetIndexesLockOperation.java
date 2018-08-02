package net.ravendb.client.documents.operations.indexes;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.node.ObjectNode;
import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.indexes.IndexLockMode;
import net.ravendb.client.documents.operations.IVoidMaintenanceOperation;
import net.ravendb.client.http.ServerNode;
import net.ravendb.client.http.VoidRavenCommand;
import net.ravendb.client.json.ContentProviderHttpEntity;
import net.ravendb.client.primitives.Reference;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.ContentType;

import java.io.IOException;
import java.util.Arrays;

public class SetIndexesLockOperation implements IVoidMaintenanceOperation {

    private final Parameters _parameters;

    public SetIndexesLockOperation(String indexName, IndexLockMode mode) {
        if (indexName == null) {
            throw new IllegalArgumentException("IndexName cannot be null");
        }

        _parameters = new Parameters();
        _parameters.setMode(mode);
        _parameters.setIndexNames(new String[]{ indexName });

        filterAutoIndexes();
    }

    public SetIndexesLockOperation(Parameters parameters) {
        if (parameters == null) {
            throw new IllegalArgumentException("Parameters cannot be null");
        }

        if (parameters.getIndexNames() == null || parameters.getIndexNames().length == 0) {
            throw new IllegalArgumentException("IndexNames cannot be null or empty");
        }

        _parameters = parameters;
        filterAutoIndexes();
    }

    private void filterAutoIndexes() {
        // Check for auto-indexes - we do not set lock for auto-indexes

        if (Arrays.stream(_parameters.getIndexNames()).anyMatch(indexName -> indexName.toLowerCase().startsWith("auto/"))) {
            throw new IllegalArgumentException("Indexes list contains Auto-Indexes. Lock Mode is not set for Auto-Indexes.");
        }
    }

    @Override
    public VoidRavenCommand getCommand(DocumentConventions conventions) {
        return new SetIndexLockCommand(conventions, _parameters);
    }

    private static class SetIndexLockCommand extends VoidRavenCommand {

        private ObjectNode _parameters;

        public SetIndexLockCommand(DocumentConventions conventions, Parameters parameters) {
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
            url.value = node.getUrl() + "/databases/" + node.getDatabase() + "/indexes/set-lock";

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
        private IndexLockMode mode;

        public String[] getIndexNames() {
            return indexNames;
        }

        public void setIndexNames(String[] indexNames) {
            this.indexNames = indexNames;
        }

        public IndexLockMode getMode() {
            return mode;
        }

        public void setMode(IndexLockMode mode) {
            this.mode = mode;
        }
    }
}
