package net.ravendb.client.documents.commands.batches;

import com.fasterxml.jackson.core.JsonGenerator;
import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.http.RavenCommand;
import net.ravendb.client.http.ServerNode;
import net.ravendb.client.json.ContentProviderHttpEntity;
import net.ravendb.client.json.JsonArrayResult;
import net.ravendb.client.primitives.CleanCloseable;
import net.ravendb.client.primitives.Reference;
import net.ravendb.client.util.TimeUtils;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.ContentType;

import java.io.IOException;
import java.util.List;

public class BatchCommand extends RavenCommand<JsonArrayResult> implements CleanCloseable {

    private final List<ICommandData> _commands;
    //TBD: attachments private readonly HashSet<Stream> _attachmentStreams;
    private final BatchOptions _options;

    public BatchCommand(DocumentConventions conventions, List<ICommandData> commands) {
        this(conventions, commands, null);
    }

    public BatchCommand(DocumentConventions conventions, List<ICommandData> commands, BatchOptions options) {
        super(JsonArrayResult.class);
        this._commands = commands;
        this._options = options;

        if (conventions == null) {
            throw new IllegalArgumentException("conventions cannot be null");
        }

        if (commands == null) {
            throw new IllegalArgumentException("commands cannot be null");
        }

        /* TBD: attachments
            for (var i = 0; i < commands.Count; i++)
            {
                var command = commands[i];
                _commands[i] = context.ReadObject(command.ToJson(conventions, context), "command");

                if (command is PutAttachmentCommandData putAttachmentCommandData)
                {
                    if (_attachmentStreams == null)
                        _attachmentStreams = new HashSet<Stream>();

                    var stream = putAttachmentCommandData.Stream;
                    PutAttachmentCommandHelper.ValidateStream(stream);
                    if (_attachmentStreams.Add(stream) == false)
                        PutAttachmentCommandHelper.ThrowStreamAlready();
                }
            }
            */
    }

    @Override
    public HttpRequestBase createRequest(ServerNode node, Reference<String> url) {
        HttpPost request = new HttpPost();

        request.setEntity(new ContentProviderHttpEntity(outputStream -> {
            try (JsonGenerator generator = mapper.getFactory().createGenerator(outputStream)) {

                generator.writeStartObject();
                generator.writeFieldName("Commands");
                generator.writeStartArray();

                for (ICommandData command : _commands) {
                    command.serialize(generator, mapper.getSerializerProviderInstance());
                }

                generator.writeEndArray();
                generator.writeEndObject();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }


            /* TBD: attachments

            if (_attachmentStreams != null && _attachmentStreams.Count > 0)
            {
                var multipartContent = new MultipartContent {request.Content};
                foreach (var stream in _attachmentStreams)
                {
                    PutAttachmentCommandHelper.PrepareStream(stream);
                    var streamContent = new AttachmentStreamContent(stream, CancellationToken);
                    streamContent.Headers.TryAddWithoutValidation("Command-Type", "AttachmentStream");
                    multipartContent.Add(streamContent);
                }
                request.Content = multipartContent;
            }
             */

        }, ContentType.APPLICATION_JSON));


        StringBuilder sb = new StringBuilder(node.getUrl() + "/databases/" + node.getDatabase() + "/bulk_docs");
        appendOptions(sb);

        url.value = sb.toString();
        return request;
    }

    @Override
    public void setResponse(String response, boolean fromCache) throws IOException {
        if (response == null) {
            throw new IllegalStateException("Got null response from the server after doing a batch, something is very wrong. Probably a garbled response.");
        }

        result = mapper.readValue(response, JsonArrayResult.class);
    }

    private void appendOptions(StringBuilder sb) {
        if (_options == null) {
            return;
        }

        sb.append("?");

        if (_options.isWaitForReplicas()) {
            sb.append("&waitForReplicasTimeout=")
                    .append(TimeUtils.durationToTimeSpan(_options.getWaitForIndexesTimeout()));

            if (_options.isThrowOnTimeoutInWaitForReplicas()) {
                sb.append("&throwOnTimeoutInWaitForReplicas=true");
            }

            sb.append("&numberOfReplicasToWaitFor=");
            sb.append(_options.isMajority() ? "majority" : _options.getNumberOfReplicasToWaitFor());
        }

        if (_options.isWaitForIndexes()) {
            sb.append("&waitForIndexesTimeout=")
                        .append(TimeUtils.durationToTimeSpan(_options.getWaitForIndexesTimeout()));

            if (_options.isThrowOnTimeoutInWaitForIndexes()) {
                sb.append("&waitForIndexThrow=true");
            }

            if (_options.getWaitForSpecificIndexes() != null) {
                for (String specificIndex : _options.getWaitForSpecificIndexes()) {
                    sb.append("&waitForSpecificIndex=").append(specificIndex);
                }
            }
        }
    }


    @Override
    public boolean isReadRequest() {
        return false;
    }

    @Override
    public void close() {
        // empty
    }
}
