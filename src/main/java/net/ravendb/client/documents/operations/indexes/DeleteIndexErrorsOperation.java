package net.ravendb.client.documents.operations.indexes;

import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.operations.IVoidMaintenanceOperation;
import net.ravendb.client.http.ServerNode;
import net.ravendb.client.http.VoidRavenCommand;
import org.apache.hc.client5.http.classic.methods.HttpDelete;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;

/**
 * Deletes indexing errors using the {@code DeleteIndexErrorsOperation}.
 * This operation clears the errors associated with the index but does not change
 * the index's state from 'Error' to 'Normal'.
 *
 * <p><strong>Note:</strong> This operation is executed only on the server node
 * as defined by the current client configuration.</p>
 */
public class DeleteIndexErrorsOperation implements IVoidMaintenanceOperation {

    private final String[] _indexNames;
    /**
     * Inherits documentation from {@link DeleteIndexErrorsOperation}.
     */
    public DeleteIndexErrorsOperation() {
        _indexNames = null;
    }
    /**
     * Inherits documentation from {@link DeleteIndexErrorsOperation}.
     *
     * @param indexNames An array of index names for which the errors should be deleted.
     */
    public DeleteIndexErrorsOperation(String[] indexNames) {
        _indexNames = indexNames;
    }

    @Override
    public VoidRavenCommand getCommand(DocumentConventions conventions) {
        return new DeleteIndexErrorsCommand(_indexNames);
    }

    private static class DeleteIndexErrorsCommand extends VoidRavenCommand {
        private final String[] _indexNames;

        public DeleteIndexErrorsCommand(String[] indexNames) {
            _indexNames = indexNames;
        }

        @Override
        public HttpUriRequestBase createRequest(ServerNode node) {
            StringBuilder url = new StringBuilder(node.getUrl() + "/databases/" + node.getDatabase() + "/indexes/errors");

            if (_indexNames != null && _indexNames.length > 0) {
                url.append("?");

                for (String indexName : _indexNames) {
                    url.append("&name=").append(urlEncode(indexName));
                }
            }

            return new HttpDelete(url.toString());
        }

        @Override
        public boolean isReadRequest() {
            return false;
        }
    }
}
