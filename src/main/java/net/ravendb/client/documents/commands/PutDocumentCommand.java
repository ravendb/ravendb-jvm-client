package net.ravendb.client.documents.commands;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.node.ObjectNode;
import net.ravendb.client.documents.commands.batches.PutResult;
import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.http.RavenCommand;
import net.ravendb.client.http.ServerNode;
import net.ravendb.client.json.ContentProviderHttpEntity;
import org.apache.hc.client5.http.classic.methods.HttpPut;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.core5.http.ContentType;

import java.io.IOException;

public class PutDocumentCommand extends RavenCommand<PutResult> {

    private final DocumentConventions _conventions;
    private final String _id;
    private final String _changeVector;
    private final ObjectNode _document;

    public PutDocumentCommand(DocumentConventions conventions, String id, String changeVector, ObjectNode document) {
        super(PutResult.class);
        if (id == null) {
            throw new IllegalArgumentException("Id cannot be null");
        }

        if (document == null) {
            throw new IllegalArgumentException("Document cannot be null");
        }

        _conventions = conventions;
        _id = id;
        _changeVector = changeVector;
        _document = document;
    }

    @Override
    public HttpUriRequestBase createRequest(ServerNode node) {
        String url = node.getUrl() + "/databases/" + node.getDatabase() + "/docs?id=" + urlEncode(_id);

        HttpPut request = new HttpPut(url);

        request.setEntity(new ContentProviderHttpEntity(outputStream -> {
            try (JsonGenerator generator = createSafeJsonGenerator(outputStream)) {
                generator.writeTree(_document);
            }
        }, ContentType.APPLICATION_JSON, _conventions));
        addChangeVectorIfNotNull(_changeVector, request);

        return request;
    }

    @Override
    public void setResponse(String response, boolean fromCache) throws IOException {
        result = mapper.readValue(response, PutResult.class);
    }

    @Override
    public boolean isReadRequest() {
        return false;
    }
}
