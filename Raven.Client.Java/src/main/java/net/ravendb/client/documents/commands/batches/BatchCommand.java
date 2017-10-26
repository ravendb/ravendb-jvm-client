package net.ravendb.client.documents.commands.batches;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.http.RavenCommand;
import net.ravendb.client.http.ServerNode;
import net.ravendb.client.json.ContentProviderHttpEntity;
import net.ravendb.client.json.JsonArrayResult;
import net.ravendb.client.primitives.CleanCloseable;
import net.ravendb.client.primitives.Reference;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.ContentType;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;

public class BatchCommand extends RavenCommand<JsonArrayResult> implements CleanCloseable {

    private final List<ICommandData> _commands;
    //TODO: private readonly HashSet<Stream> _attachmentStreams;
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

        /* TODO:
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
        /* TODO:

            Timeout = options?.RequestTimeout;
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
                //TODO:
                throw new RuntimeException(e);
            }


            /* TODO

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
        //TODO: AppendOptions(sb);

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

    /*
    TODO

        private void AppendOptions(StringBuilder sb)
        {
            if (_options == null)
                return;

            sb.AppendLine("?");

            if (_options.WaitForReplicas)
            {
                sb.Append("&waitForReplicasTimeout=").Append(_options.WaitForReplicasTimeout);

                if (_options.ThrowOnTimeoutInWaitForReplicas)
                    sb.Append("&throwOnTimeoutInWaitForReplicas=true");

                sb.Append("&numberOfReplicasToWaitFor=");
                sb.Append(_options.Majority
                    ? "majority"
                    : _options.NumberOfReplicasToWaitFor.ToString());
            }

            if (_options.WaitForIndexes)
            {
                sb.Append("&waitForIndexesTimeout=").Append(_options.WaitForIndexesTimeout);
                if (_options.ThrowOnTimeoutInWaitForIndexes)
                {
                    sb.Append("&waitForIndexThrow=true");
                }
                if (_options.WaitForSpecificIndexes != null)
                {
                    foreach (var specificIndex in _options.WaitForSpecificIndexes)
                    {
                        sb.Append("&waitForSpecificIndex=").Append(specificIndex);
                    }
                }
            }
        }*/

    @Override
    public boolean isReadRequest() {
        return false;
    }

    @Override
    public void close() {
        // empty
    }
}
