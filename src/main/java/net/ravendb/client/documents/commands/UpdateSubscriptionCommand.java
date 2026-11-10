package net.ravendb.client.documents.commands;

import com.fasterxml.jackson.core.JsonGenerator;
import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.subscriptions.SubscriptionUpdateOptions;
import net.ravendb.client.documents.subscriptions.UpdateSubscriptionResult;
import net.ravendb.client.http.IRaftCommand;
import net.ravendb.client.http.RavenCommand;
import net.ravendb.client.http.ServerNode;
import net.ravendb.client.json.ContentProviderHttpEntity;
import net.ravendb.client.util.RaftIdGenerator;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.core5.http.ContentType;

import java.io.IOException;

public class UpdateSubscriptionCommand extends RavenCommand<UpdateSubscriptionResult> implements IRaftCommand {
    private final SubscriptionUpdateOptions _options;
    private final DocumentConventions _conventions;

    public UpdateSubscriptionCommand(DocumentConventions conventions, SubscriptionUpdateOptions options) {
        super(UpdateSubscriptionResult.class);
        _options = options;
        _conventions = conventions;
    }

    @Override
    public HttpUriRequestBase createRequest(ServerNode node) {
        String url = node.getUrl() + "/databases/" + node.getDatabase() + "/subscriptions/update";

        HttpPost request = new HttpPost(url);
        request.setEntity(new ContentProviderHttpEntity(outputStream -> {
            try (JsonGenerator generator = createSafeJsonGenerator(outputStream)) {
                writeSubscriptionUpdateOptions(generator, _options);
            }
        }, ContentType.APPLICATION_JSON, _conventions));

        return request;
    }

    private static void writeSubscriptionUpdateOptions(JsonGenerator generator, SubscriptionUpdateOptions options) throws IOException {
        generator.writeStartObject();

        if (options.getId() != null) {
            generator.writeNumberField("Id", options.getId());
        }

        if (options.isPinToMentorNodeWasSet()) {
            generator.writeBooleanField("PinToMentorNode", options.isPinToMentorNode());
        }

        generator.writeBooleanField("CreateNew", options.isCreateNew());
        generator.writeStringField("Name", options.getName());
        generator.writeStringField("Query", options.getQuery());
        generator.writeStringField("ChangeVector", options.getChangeVector());
        generator.writeStringField("MentorNode", options.getMentorNode());

        if (options.isDisabledWasSet()) {
            generator.writeBooleanField("Disabled", options.isDisabled());
        }

        generator.writeEndObject();
    }

    @Override
    public void setResponse(String response, boolean fromCache) throws IOException {
        if (fromCache) {
            result = new UpdateSubscriptionResult();
            result.setName(_options.getName());

            return;
        }

        if (response == null) {
            throwInvalidResponse();
        }

        result = mapper.readValue(response, resultClass);
    }

    @Override
    public boolean isReadRequest() {
        return false;
    }

    private final String raftUniqueRequestId = RaftIdGenerator.newId();

    @Override
    public String getRaftUniqueRequestId() {
        return raftUniqueRequestId;
    }
}
