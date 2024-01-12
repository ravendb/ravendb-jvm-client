package net.ravendb.client.documents.commands;

import net.ravendb.client.http.ServerNode;
import net.ravendb.client.http.VoidRavenCommand;
import org.apache.hc.client5.http.classic.methods.HttpDelete;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;

public class DeleteDocumentCommand extends VoidRavenCommand {
    private final String _id;
    private final String _changeVector;

    public DeleteDocumentCommand(String id) {
        this(id, null);
    }

    public DeleteDocumentCommand(String id, String changeVector) {
        if (id == null) {
            throw new IllegalArgumentException("Id cannot be null");
        }

        _id = id;
        _changeVector = changeVector;
    }

    @Override
    public HttpUriRequestBase createRequest(ServerNode node) {
        ensureIsNotNullOrString(_id, "id");

        String url = node.getUrl() + "/databases/" + node.getDatabase() + "/docs?id=" + urlEncode(_id);

        HttpDelete request = new HttpDelete(url);
        addChangeVectorIfNotNull(_changeVector, request);
        return request;
    }
}
