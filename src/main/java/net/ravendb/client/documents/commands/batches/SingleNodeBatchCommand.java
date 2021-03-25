package net.ravendb.client.documents.commands.batches;

import com.fasterxml.jackson.core.JsonGenerator;
import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.http.RavenCommand;
import net.ravendb.client.http.ServerNode;
import net.ravendb.client.json.BatchCommandResult;
import net.ravendb.client.json.ContentProviderHttpEntity;
import net.ravendb.client.primitives.CleanCloseable;
import net.ravendb.client.primitives.Reference;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.ContentType;
import java.io.IOException;
import java.util.List;

public class SingleNodeBatchCommand extends RavenCommand<BatchCommandResult> implements CleanCloseable {
    private final DocumentConventions _conventions;
    private final List<ICommandData> _commands;

    public SingleNodeBatchCommand(DocumentConventions conventions, List<ICommandData> commands) {
        super(BatchCommandResult.class);
        this._commands = commands;
        this._conventions = conventions;

        if (conventions == null) {
            throw new IllegalArgumentException("conventions cannot be null");
        }

        if (commands == null) {
            throw new IllegalArgumentException("commands cannot be null");
        }


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
                    command.serialize(generator, _conventions);
                }

                generator.writeEndArray();

                generator.writeEndObject();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
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

        result = mapper.readValue(response, BatchCommandResult.class);
    }

    private void appendOptions(StringBuilder sb) {

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
