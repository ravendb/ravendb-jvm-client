package net.ravendb.client.documents.commands;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.node.ObjectNode;
import net.ravendb.client.documents.commands.batches.PutResult;
import net.ravendb.client.http.RavenCommand;
import net.ravendb.client.http.ServerNode;
import net.ravendb.client.json.ContentProviderHttpEntity;
import net.ravendb.client.primitives.Reference;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.ContentType;

import java.io.IOException;

public class PutDocumentCommand extends RavenCommand<PutResult> {

    private final String _id;
    private final String _changeVector;
    private final ObjectNode _document;

    public PutDocumentCommand(String id, String changeVector, ObjectNode document) {
        super(PutResult.class);
        if (id == null) {
            throw new IllegalArgumentException("Id cannot be null");
        }

        if (document == null) {
            throw new IllegalArgumentException("Document cannot be null");
        }

        _id = id;
        _changeVector = changeVector;
        _document = document;
    }

    @Override
    public HttpRequestBase createRequest(ServerNode node, Reference<String> url) {
        url.value = node.getUrl() + "/databases/" + node.getDatabase() + "/docs?id=" + urlEncode(_id);

        HttpPut request = new HttpPut();

        request.setEntity(new ContentProviderHttpEntity(outputStream -> {
            try (JsonGenerator generator = mapper.getFactory().createGenerator(outputStream)) {
                generator.writeTree(_document);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }, ContentType.APPLICATION_JSON));
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
