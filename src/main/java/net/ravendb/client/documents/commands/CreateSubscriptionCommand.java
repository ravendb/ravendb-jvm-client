package net.ravendb.client.documents.commands;

import com.fasterxml.jackson.core.JsonGenerator;
import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.subscriptions.CreateSubscriptionResult;
import net.ravendb.client.documents.subscriptions.SubscriptionCreationOptions;
import net.ravendb.client.http.RavenCommand;
import net.ravendb.client.http.ServerNode;
import net.ravendb.client.json.ContentProviderHttpEntity;
import net.ravendb.client.primitives.Reference;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.ContentType;

import java.io.IOException;

public class CreateSubscriptionCommand extends RavenCommand<CreateSubscriptionResult> {

    @SuppressWarnings("FieldCanBeLocal")
    private final DocumentConventions _conventions;
    private final SubscriptionCreationOptions _options;
    private final String _id;

    public CreateSubscriptionCommand(DocumentConventions conventions, SubscriptionCreationOptions options) {
        this(conventions, options, null);
    }

    public CreateSubscriptionCommand(DocumentConventions conventions, SubscriptionCreationOptions options, String id) {
        super(CreateSubscriptionResult.class);
        _conventions = conventions;
        _options = options;
        _id = id;
    }

    @Override
    public HttpRequestBase createRequest(ServerNode node, Reference<String> url) {
        url.value = node.getUrl() + "/databases/" + node.getDatabase() + "/subscriptions";

        if (_id != null) {
            url.value += "?id=" + _id;
        }

        HttpPut request = new HttpPut();
        request.setEntity(new ContentProviderHttpEntity(outputStream -> {
            try (JsonGenerator generator = mapper.getFactory().createGenerator(outputStream)) {
                generator.getCodec().writeValue(generator, _options);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }, ContentType.APPLICATION_JSON));

        return request;
    }

    @Override
    public void setResponse(String response, boolean fromCache) throws IOException {
        result = mapper.readValue(response, CreateSubscriptionResult.class);
    }

    @Override
    public boolean isReadRequest() {
        return false;
    }
}
