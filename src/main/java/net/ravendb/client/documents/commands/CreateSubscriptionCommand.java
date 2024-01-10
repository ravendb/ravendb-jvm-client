package net.ravendb.client.documents.commands;

import com.fasterxml.jackson.core.JsonGenerator;
import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.subscriptions.CreateSubscriptionResult;
import net.ravendb.client.documents.subscriptions.SubscriptionCreationOptions;
import net.ravendb.client.http.IRaftCommand;
import net.ravendb.client.http.RavenCommand;
import net.ravendb.client.http.ServerNode;
import net.ravendb.client.json.ContentProviderHttpEntity;
import net.ravendb.client.util.RaftIdGenerator;
import org.apache.hc.client5.http.classic.methods.HttpPut;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.core5.http.ContentType;

import java.io.IOException;

public class CreateSubscriptionCommand extends RavenCommand<CreateSubscriptionResult> implements IRaftCommand {

    private final SubscriptionCreationOptions _options;
    private final String _id;
    private final DocumentConventions _conventions;

    public CreateSubscriptionCommand(DocumentConventions conventions, SubscriptionCreationOptions options) {
        this(conventions, options, null);
    }

    public CreateSubscriptionCommand(DocumentConventions conventions, SubscriptionCreationOptions options, String id) {
        super(CreateSubscriptionResult.class);
        if (options == null) {
            throw new IllegalArgumentException("Options cannot be null");
        }
        _conventions = conventions;
        _options = options;
        _id = id;
    }

    @Override
    public HttpUriRequestBase createRequest(ServerNode node) {
        String url = node.getUrl() + "/databases/" + node.getDatabase() + "/subscriptions";

        if (_id != null) {
            url += "?id=" + urlEncode(_id);
        }

        HttpPut request = new HttpPut(url);
        request.setEntity(new ContentProviderHttpEntity(outputStream -> {
            try (JsonGenerator generator = createSafeJsonGenerator(outputStream)) {
                generator.getCodec().writeValue(generator, _options);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }, ContentType.APPLICATION_JSON, _conventions));

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

    @Override
    public String getRaftUniqueRequestId() {
        return RaftIdGenerator.newId();
    }
}
