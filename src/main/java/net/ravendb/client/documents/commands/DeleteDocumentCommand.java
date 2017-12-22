package net.ravendb.client.documents.commands;

import net.ravendb.client.http.ServerNode;
import net.ravendb.client.http.VoidRavenCommand;
import net.ravendb.client.primitives.Reference;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpRequestBase;

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
    public HttpRequestBase createRequest(ServerNode node, Reference<String> url) {
        ensureIsNotNullOrString(_id, "id");

        url.value = node.getUrl() + "/databases/" + node.getDatabase() + "/docs?id=" + urlEncode(_id);

        HttpDelete request = new HttpDelete();
        addChangeVectorIfNotNull(_changeVector, request);
        return request;
    }
}
