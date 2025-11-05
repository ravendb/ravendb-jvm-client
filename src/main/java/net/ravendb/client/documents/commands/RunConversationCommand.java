package net.ravendb.client.documents.commands;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.operations.AI.agents.ConversationResult;
import net.ravendb.client.documents.operations.AI.agents.RunConversationOperation;
import net.ravendb.client.http.IRaftCommand;
import net.ravendb.client.http.RavenCommand;
import net.ravendb.client.http.RavenCommandResponseType;
import net.ravendb.client.http.ServerNode;
import net.ravendb.client.json.ContentProviderHttpEntity;
import net.ravendb.client.util.UrlUtils;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.ContentType;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import static net.ravendb.client.extensions.JsonExtensions.createDefaultJsonSerializer;


public class RunConversationCommand<TAnswer>
        extends RavenCommand<ConversationResult<TAnswer>>
        implements IRaftCommand {

    private final RunConversationOperation<TAnswer> parent;
    private final DocumentConventions conventions;
    private String raftId;

    public RunConversationCommand(RunConversationOperation<TAnswer> parent, DocumentConventions conventions) {
        super((Class<ConversationResult<TAnswer>>) (Class<?>) ConversationResult.class);
        this.conventions = conventions;
        this.parent = parent;

        if (parent.getStreamPropertyPath() != null)
            this.responseType = RavenCommandResponseType.RAW;
    }

    @Override
    public boolean isReadRequest() {
        return false;
    }

    @Override
    public HttpUriRequestBase createRequest(ServerNode node) {
        StringBuilder uriBuilder = new StringBuilder();
        uriBuilder.append(node.getUrl())
                .append("/databases/")
                .append(node.getDatabase())
                .append("/ai/agent?")
                .append("conversationId=").append(UrlUtils.escapeDataString(this.parent.getConversationId()))
                .append("&agentId=").append(UrlUtils.escapeDataString(this.parent.getAgentId()));

        if (this.parent.getConversationId().charAt(this.parent.getConversationId().length() - 1) == '|') {
            this.raftId = UUID.randomUUID().toString();
        }

        if (this.parent.getChangeVector() != null && !this.parent.getChangeVector().isEmpty()) {
            uriBuilder.append("&changeVector=").append(UrlUtils.escapeDataString(this.parent.getChangeVector()));
        }
        if (this.parent.getStreamPropertyPath() != null) {
            uriBuilder.append("&streamPropertyPath=").append(UrlUtils.escapeDataString(this.parent.getStreamPropertyPath()));
            uriBuilder.append("&streaming=true");
        }

        HttpPost request = new HttpPost(uriBuilder.toString());

        request.setEntity(new ContentProviderHttpEntity(outputStream -> {
            try (JsonGenerator generator = createSafeJsonGenerator(outputStream)) {
                ObjectNode bodyObj = mapper.createObjectNode();
                bodyObj.set("ActionResponses", mapper.valueToTree(this.parent.getActionResponses()));
                bodyObj.set("UserPrompt", mapper.valueToTree(this.parent.getPromptParts()));
                bodyObj.set("CreationOptions", mapper.valueToTree(this.parent.getOptions()));
                generator.writeTree(bodyObj);
            }
        }, ContentType.APPLICATION_JSON,conventions));

        return request;
    }

    @Override
    public String getRaftUniqueRequestId() {
        return raftId;
    }

    @Override
    public CompletableFuture<String> setResponseAsync(InputStream bodyStream, boolean fromCache) {
        if (bodyStream == null ) {
            this.throwInvalidResponse();
        }

        if (this.parent.getStreamPropertyPath() != null  && this.parent.getStreamCallback() != null) {
            return processStreamingResponse(bodyStream);
        }
        return this.parseResponseDefaultAsync(bodyStream);
    }

    @Override
    public void setResponseRaw(ClassicHttpResponse response, InputStream stream) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, "UTF-8"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();

                if (line.startsWith("{")) {
                    this.result = mapper.readValue(line, ConversationResult.class);
                    break;
                }

                String unescaped = mapper.readValue(line, String.class);

                if (this.parent.getStreamCallback() != null) {
                    this.parent.getStreamCallback().onChunk(unescaped).get();
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to read conversation stream", e);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void setResponse(String response, boolean fromCache) throws IOException {
        ObjectMapper mapper = createDefaultJsonSerializer();
        this.result = mapper.readValue(response, ConversationResult.class);
    }

    private CompletableFuture<String> parseResponseDefaultAsync(InputStream bodyStream) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String body = new BufferedReader(new InputStreamReader(bodyStream, StandardCharsets.UTF_8))
                        .lines()
                        .collect(Collectors.joining("\n"));

                this.result = parseAndTransform(body, new TypeReference<ConversationResult<TAnswer>>() {});
                return body;
            } catch (Exception e) {
                throw new CompletionException(e);
            }
        });
    }

    private CompletableFuture<String> processStreamingResponse(InputStream bodyStream) {
        return CompletableFuture.supplyAsync(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(bodyStream, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.trim().isEmpty()) continue;

                    if (line.startsWith("{")) {
                        this.result = parseAndTransform(line, new TypeReference<ConversationResult<TAnswer>>() {});
                        return line;
                    }

                    try {
                        Object parsed = new ObjectMapper().readValue(line, Object.class);
                        String chunk;
                        if (parsed instanceof String) {
                            chunk = (String) parsed;
                        } else {
                            chunk = new ObjectMapper().writeValueAsString(parsed);
                        }
                        this.parent.getStreamCallback().onChunk(chunk).get();
                    } catch (Exception e) {
                        this.parent.getStreamCallback().onChunk(line).get();
                    }
                }

                if (this.result == null) {
                    throw new IllegalStateException("No final result received in streaming response");
                }

                return null;
            } catch (Exception e) {
                throw new CompletionException(e);
            }
        });
    }
}
