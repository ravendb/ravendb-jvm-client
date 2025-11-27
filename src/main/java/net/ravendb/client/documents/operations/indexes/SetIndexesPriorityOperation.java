package net.ravendb.client.documents.operations.indexes;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.node.ObjectNode;
import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.indexes.IndexPriority;
import net.ravendb.client.documents.operations.IVoidMaintenanceOperation;
import net.ravendb.client.http.IRaftCommand;
import net.ravendb.client.http.ServerNode;
import net.ravendb.client.http.VoidRavenCommand;
import net.ravendb.client.json.ContentProviderHttpEntity;
import net.ravendb.client.util.RaftIdGenerator;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.core5.http.ContentType;

/**
 * Adjusts the priority of index threads using the {@code SetIndexesPriorityOperation}.
 * Each index operates on its own dedicated thread, and this operation allows you to raise or lower the thread's priority.
 * By default, RavenDB assigns a lower priority to indexing threads compared to request-processing threads.
 *
 * <p><strong>Indexes scope:</strong> The priority can be set for both static and auto indexes.</p>
 * <p><strong>Nodes scope:</strong> The priority is updated on all nodes within the database group.</p>
 */
public class SetIndexesPriorityOperation implements IVoidMaintenanceOperation {

    private final Parameters _parameters;

    /**
     * Inherits documentation from {@link SetIndexesPriorityOperation}.
     *
     * @param indexName The name of the index for which the priority is being modified.
     * @param priority  The priority level to set for the index. Valid values are Low, Normal, and High.
     */
    public SetIndexesPriorityOperation(String indexName, IndexPriority priority) {
        if (indexName == null) {
            throw new IllegalArgumentException("IndexName cannot be null");
        }

        _parameters = new Parameters();
        _parameters.setPriority(priority);
        _parameters.setIndexNames(new String[]{ indexName });
    }
    /**
     * Inherits documentation from {@link SetIndexesPriorityOperation}.
     *
     * @param parameters The Parameters object containing the list of index names and the priority level to apply.
     */
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

    private static class SetIndexPriorityCommand extends VoidRavenCommand implements IRaftCommand {
        private final ObjectNode _parameters;
        private final DocumentConventions _conventions;

        public SetIndexPriorityCommand(DocumentConventions conventions, Parameters parameters) {
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
            String url = node.getUrl() + "/databases/" + node.getDatabase() + "/indexes/set-priority";

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
     * Represents the parameters required to set the priority level for multiple indexes.
     * This class includes the list of index names and the priority level to apply.
     */
    public static class Parameters {
        /**
         * An array of index names for which the priority is being modified.
         */
        private String[] indexNames;
        /**
         * The priority level to apply to the specified indexes. Valid values are Low, Normal, and High.
         */
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
