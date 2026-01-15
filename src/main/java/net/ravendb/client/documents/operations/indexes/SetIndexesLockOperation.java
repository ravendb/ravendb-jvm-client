package net.ravendb.client.documents.operations.indexes;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.node.ObjectNode;
import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.indexes.IndexLockMode;
import net.ravendb.client.documents.operations.IVoidMaintenanceOperation;
import net.ravendb.client.http.IRaftCommand;
import net.ravendb.client.http.ServerNode;
import net.ravendb.client.http.VoidRavenCommand;
import net.ravendb.client.json.ContentProviderHttpEntity;
import net.ravendb.client.util.RaftIdGenerator;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.core5.http.ContentType;

import java.util.Arrays;

/**
 * Modifies the lock mode of one or more indexes using the {@code SetIndexesLockOperation}.
 * The lock mode controls how index modifications are handled and can only be applied to static indexes, not auto-indexes.
 *
 * <p><strong>Note:</strong> The lock mode is updated on all nodes within the database group.</p>
 */
public class SetIndexesLockOperation implements IVoidMaintenanceOperation {

    private final Parameters _parameters;
    /**
     * Inherits documentation from {@link SetIndexesLockOperation}.
     *
     * @param indexName The name of the index for which the lock mode is being modified.
     * @param mode      The lock mode to be set for the index. Valid values are Unlock, LockedIgnore, and LockedError.
     */
    public SetIndexesLockOperation(String indexName, IndexLockMode mode) {
        if (indexName == null) {
            throw new IllegalArgumentException("IndexName cannot be null");
        }

        _parameters = new Parameters();
        _parameters.setMode(mode);
        _parameters.setIndexNames(new String[]{ indexName });

        filterAutoIndexes();
    }

    /**
     * Inherits documentation from {@link SetIndexesLockOperation}.
     *
     * @param parameters The Parameters object containing the list of index names and the lock mode to apply.
     */
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

    private static class SetIndexLockCommand extends VoidRavenCommand implements IRaftCommand {

        private ObjectNode _parameters;
        private final DocumentConventions _conventions;

        public SetIndexLockCommand(DocumentConventions conventions, Parameters parameters) {
            if (conventions == null) {
                throw new IllegalArgumentException("Conventions cannot be null");
            }

            if (parameters == null) {
                throw new IllegalArgumentException("Parameters cannot be null");
            }

            _conventions = conventions;
            _parameters = mapper.valueToTree(parameters);
        }

        @Override
        public HttpUriRequestBase createRequest(ServerNode node) {
            String url = node.getUrl() + "/databases/" + node.getDatabase() + "/indexes/set-lock";

            HttpPost request = new HttpPost(url);

            request.setEntity(new ContentProviderHttpEntity(outputStream -> {
                try (JsonGenerator generator = createSafeJsonGenerator(outputStream)) {
                    generator.writeTree(_parameters);
                }
            }, ContentType.APPLICATION_JSON, _conventions));

            return request;
        }

        @Override
        public String getRaftUniqueRequestId() {
            return RaftIdGenerator.newId();
        }
    }

    /**
     * Represents the parameters required to set the lock mode for multiple indexes.
     * This class includes the list of index names and the lock mode to apply.
     */
    public static class Parameters {
        /**
         * An array of index names for which the lock mode is being modified.
         */
        private String[] indexNames;
        /**
         * The lock mode to be applied to the specified indexes. Valid values are Unlock, LockedIgnore, and LockedError.
         */
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
