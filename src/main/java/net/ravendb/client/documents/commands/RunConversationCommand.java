package net.ravendb.client.documents.commands;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.operations.AI.AiStreamCallback;
import net.ravendb.client.documents.operations.AI.agents.AiAgentActionResponse;
import net.ravendb.client.documents.operations.AI.agents.AiConversationCreationOptions;
import net.ravendb.client.documents.operations.AI.agents.ConversationResult;
import net.ravendb.client.http.IRaftCommand;
import net.ravendb.client.http.RavenCommand;
import net.ravendb.client.http.RavenCommandResponseType;
import net.ravendb.client.http.ServerNode;
import net.ravendb.client.util.RaftIdGenerator;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.stream.Collectors;

public class RunConversationCommand<TAnswer>
        extends RavenCommand<ConversationResult<TAnswer>>
        implements IRaftCommand {

    private final String conversationId;
    private final String agentId;
    private final String prompt;
    private final List<AiAgentActionResponse> actionResponses;
    private final AiConversationCreationOptions options;
    private final String changeVector;
    private final String streamPropertyPath;
    private final AiStreamCallback streamCallback;
    private String raftId;

    public RunConversationCommand(
            String conversationId,
            String agentId,
            String prompt,
            List<AiAgentActionResponse> actionResponses,
            AiConversationCreationOptions options,
            String changeVector,
            DocumentConventions conventions,
            String streamPropertyPath,
            AiStreamCallback streamCallback){
        super((Class<ConversationResult<TAnswer>>) (Class<?>) ConversationResult.class);
        this.conversationId = conversationId;
        this.agentId = agentId;
        this.prompt = prompt;
        this.actionResponses = actionResponses;
        this.options = options;
        this.changeVector = changeVector;
        this.streamPropertyPath = streamPropertyPath;
        this.streamCallback = streamCallback;

        if (this.streamPropertyPath != null && this.streamCallback != null) {
            this.responseType = RavenCommandResponseType.RAW;
        }

        if (conversationId != null && conversationId.endsWith("|")) {
            this.raftId = RaftIdGenerator.newId();
        }
    }

    @Override
    public boolean isReadRequest() {
        return false;
    }

    @Override
    public String getRaftUniqueRequestId() {
        return raftId;
    }

    @Override
    public HttpUriRequestBase createRequest(ServerNode node) {
        return null;
//        StringBuilder uriBuilder = new StringBuilder();
//        uriBuilder.append(node.getUrl())
//                .append("/databases/")
//                .append(node.getDatabase())
//                .append("/ai/agent?")
//                .append("conversationId=").append(UrlUtils.escapeDataString(this.conversationId))
//                .append("&agentId=").append(UrlUtils.escapeDataString(this.agentId));
//
//        if (this.changeVector != null && !this.changeVector.isEmpty()) {
//            uriBuilder.append("&changeVector=").append(UrlUtils.escapeDataString(this.changeVector));
//        }

//        if (this._streamPropertyPath) {
//            uriParams.append("streaming", "true");
//            uriParams.append("streamPropertyPath", this._streamPropertyPath);
//        }
//
//        HttpPost request = new HttpPost(uriBuilder.toString());
//
//        request.setEntity(new ContentProviderHttpEntity(outputStream -> {
//            try (JsonGenerator generator = createSafeJsonGenerator(outputStream)) {
//                ObjectNode bodyObj = mapper.createObjectNode();
//                bodyObj.set("ActionResponses", mapper.valueToTree(this.actionResponses));
//                bodyObj.put("UserPrompt", this.prompt);
//                bodyObj.set("CreationOptions", mapper.valueToTree(this.options));
//
//                // Apply PascalCase transformation with ignorePaths logic
//                ObjectNode transformed = ObjectUtils.transformObjectKeys(
//                        bodyObj,
//                        ObjectUtils.pascalCase(),
//                        Collections.singletonList(Pattern.compile("^CreationOptions\\.Parameters\\..*$"))
//                );
//
//                generator.writeTree(transformed);
//            }
//        }, ContentType.APPLICATION_JSON, _conventions));
//
//        return request;
    }

    @Override
    public CompletableFuture<String> setResponseAsync(InputStream bodyStream, boolean fromCache) {
        if (bodyStream == null ) {
            this.throwInvalidResponse();
        }

        if (this.streamPropertyPath != null  && this.streamCallback != null) {
            return processStreamingResponse(bodyStream);
        }
        return this.parseResponseDefaultAsync(bodyStream);
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
                        // Final result line
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
                        streamCallback.onChunk(chunk).get();
                    } catch (Exception e) {
                        streamCallback.onChunk(line).get();
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

