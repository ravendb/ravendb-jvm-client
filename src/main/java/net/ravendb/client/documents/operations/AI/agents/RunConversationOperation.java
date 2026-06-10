package net.ravendb.client.documents.operations.AI.agents;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import net.ravendb.client.documents.AI.AiConversationCreationOptions;
import net.ravendb.client.documents.AI.ContentPart;
import net.ravendb.client.documents.AI.TextPart;
import net.ravendb.client.documents.commands.batches.ICommandData;
import net.ravendb.client.documents.commands.batches.PutAttachmentCommandData;
import net.ravendb.client.documents.commands.batches.PutAttachmentCommandHelper;
import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.AI.AiStreamCallback;
import net.ravendb.client.documents.operations.IMaintenanceOperation;
import net.ravendb.client.exceptions.RavenException;
import net.ravendb.client.http.IRaftCommand;
import net.ravendb.client.http.RavenCommand;
import net.ravendb.client.http.RavenCommandResponseType;
import net.ravendb.client.http.ServerNode;
import net.ravendb.client.json.ContentProviderHttpEntity;
import net.ravendb.client.serverwide.tcp.TcpConnectionHeaderMessage;
import net.ravendb.client.util.UrlUtils;
import net.ravendb.client.util.ValidationMethods;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.client5.http.entity.mime.*;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpEntity;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import static net.ravendb.client.extensions.JsonExtensions.createDefaultJsonSerializer;

public class RunConversationOperation<TAnswer> implements IMaintenanceOperation<ConversationResult<TAnswer>> {
    private final String agentId;
    private final String conversationId;
    private final Iterable<ContentPart> promptParts;
    private final List<AiAgentActionResponse> actionResponses;
    private final AiConversationCreationOptions options;
    private final List<AiAgentArtificialActionResponse> artificialActions;
    private final String changeVector;
    private final String streamPropertyPath;
    private final AiStreamCallback streamCallback;
    private List<ICommandData> attachmentsCommands;

    public RunConversationOperation(String agentId, String conversationId, List<ContentPart> promptParts, List<AiAgentActionResponse> actionResponses, AiConversationCreationOptions options, String changeVector) {
        this(agentId, conversationId, promptParts, actionResponses, Collections.emptyList(), options, changeVector, null, null);
    }

    public RunConversationOperation(String agentId, String conversationId, List<ContentPart> promptParts, List<AiAgentActionResponse> actionResponses, List<AiAgentArtificialActionResponse> artificialActions, AiConversationCreationOptions options, String changeVector) {
        this(agentId, conversationId, promptParts, actionResponses, artificialActions, options, changeVector, null, null);
    }
    public RunConversationOperation( String agentId, String conversationId, List<ContentPart> promptParts, List<AiAgentActionResponse> actionResponses, AiConversationCreationOptions options, String changeVector, String streamPropertyPath, AiStreamCallback streamedChunksCallback) {
        this(agentId, conversationId, promptParts, actionResponses, Collections.emptyList(), options, changeVector, streamPropertyPath, streamedChunksCallback);
    }

    public RunConversationOperation( String agentId, String conversationId, Iterable<ContentPart> promptParts, List<AiAgentActionResponse> actionResponses, List<AiAgentArtificialActionResponse> artificialActions, AiConversationCreationOptions options, String changeVector, String streamPropertyPath, AiStreamCallback streamedChunksCallback) {
        ValidationMethods.assertNotNullOrEmpty(agentId, "agentId");
        ValidationMethods.assertNotNullOrEmpty(conversationId, "conversationId");
        if ((streamPropertyPath == null) != (streamedChunksCallback == null)) {
            throw new IllegalStateException( "Both streamPropertyPath and streamedChunksCallback must be specified together");
        }
        this.agentId = agentId;
        this.conversationId = conversationId;
        this.promptParts = promptParts;
        this.changeVector = changeVector;
        this.actionResponses = actionResponses;
        this.artificialActions = artificialActions;
        this.options = options;
        this.streamPropertyPath = streamPropertyPath;
        this.streamCallback = streamedChunksCallback;
    }

    public RunConversationOperation(String agentId, String conversationId, Iterable<ContentPart> promptParts, List<AiAgentActionResponse> actionResponses, List<AiAgentArtificialActionResponse> artificialActions, AiConversationCreationOptions options, String changeVector, List<ICommandData> attachmentsCommands, String streamPropertyPath, AiStreamCallback streamedChunksCallback) {
        this(agentId, conversationId, promptParts, actionResponses, artificialActions, options, changeVector, streamPropertyPath, streamedChunksCallback);
        this.attachmentsCommands = attachmentsCommands;
    }

    @Deprecated
    public RunConversationOperation(String agentId, String conversationId, String userPrompt, List<AiAgentActionResponse> actionResponses, AiConversationCreationOptions options, String changeVector, String streamPropertyPath, AiStreamCallback streamedChunksCallback) {
        this(agentId, conversationId, Arrays.asList(new TextPart(userPrompt)), actionResponses, Collections.emptyList(), options, changeVector, streamPropertyPath, streamedChunksCallback);
    }

    public TcpConnectionHeaderMessage.OperationResultType getResultType() {
        return TcpConnectionHeaderMessage.OperationResultType.CommandResult;
    }

    @Override
    public RavenCommand<ConversationResult<TAnswer>> getCommand(DocumentConventions conventions) {
        return new RunConversationCommand<TAnswer>(
                this,
                conventions
        );
    }

    public List<AiAgentArtificialActionResponse> getArtificialActions() {
        return artificialActions;
    }

    public String getStreamPropertyPath() {
        return streamPropertyPath;
    }

    public String getAgentId() {
        return agentId;
    }

    public String getConversationId() {
        return conversationId;
    }

    public String getChangeVector() {
        return changeVector;
    }

    public AiStreamCallback getStreamCallback() {
        return streamCallback;
    }

    public List<AiAgentActionResponse> getActionResponses() {
        return actionResponses;
    }

    public Iterable<ContentPart> getPromptParts() {
        return promptParts;
    }

    public AiConversationCreationOptions getOptions() {
        return options;
    }

    public List<ICommandData> getAttachmentsCommands() {
        return attachmentsCommands;
    }

    class RunConversationCommand<TAnswer>
            extends RavenCommand<ConversationResult<TAnswer>>
            implements IRaftCommand {

        private final RunConversationOperation<TAnswer> parent;
        private final DocumentConventions conventions;
        private String raftId = "";
        private LinkedHashSet<InputStream> uniqueAttachmentStreams;

        public RunConversationCommand(RunConversationOperation<TAnswer> parent, DocumentConventions conventions) {
            super((Class<ConversationResult<TAnswer>>) (Class<?>) ConversationResult.class);
            this.conventions = conventions;
            this.parent = parent;

            if (parent.getStreamPropertyPath() != null)
                this.responseType = RavenCommandResponseType.RAW;

            if (this.parent.getConversationId().charAt(this.parent.getConversationId().length() - 1) == '|') {
                this.raftId = UUID.randomUUID().toString();
            }

            if (parent.getAttachmentsCommands() != null) {
                for (ICommandData command : parent.getAttachmentsCommands()) {
                    if (command instanceof PutAttachmentCommandData) {
                        if (uniqueAttachmentStreams == null) {
                            uniqueAttachmentStreams = new LinkedHashSet<>();
                        }

                        InputStream stream = ((PutAttachmentCommandData) command).getStream();
                        if (!uniqueAttachmentStreams.add(stream)) {
                            PutAttachmentCommandHelper.throwStreamWasAlreadyUsed();
                        }
                    }
                }
            }
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
                    bodyObj.set("ArtificialActions", mapper.valueToTree(this.parent.getArtificialActions()));
                    bodyObj.set("UserPrompt", mapper.valueToTree(this.parent.getPromptParts()));
                    bodyObj.set("CreationOptions", mapper.valueToTree(this.parent.getOptions()));
                    generator.writeTree(bodyObj);
                }
            }, ContentType.APPLICATION_JSON,conventions));

            List<ICommandData> attachmentsCommands = this.parent.getAttachmentsCommands();
            if (attachmentsCommands != null && !attachmentsCommands.isEmpty()) {
                MultipartEntityBuilder entityBuilder = MultipartEntityBuilder.create();

                HttpEntity entity = request.getEntity();

                try {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    entity.writeTo(baos);

                    FormBodyPartBuilder mainPartBuilder = FormBodyPartBuilder
                            .create("main", new ByteArrayBody(baos.toByteArray(), "main"));

                    if (entity.getContentEncoding() != null) {
                        mainPartBuilder.addField("Content-Encoding", entity.getContentEncoding());
                    }

                    entityBuilder.addPart(mainPartBuilder.build());

                    ByteArrayOutputStream commandsStream = new ByteArrayOutputStream();
                    try (JsonGenerator generator = createSafeJsonGenerator(commandsStream)) {
                        generator.writeStartObject();
                        generator.writeFieldName("Commands");
                        generator.writeStartArray();

                        for (ICommandData command : attachmentsCommands) {
                            command.serialize(generator, conventions);
                        }

                        generator.writeEndArray();
                        generator.writeEndObject();
                    }

                    entityBuilder.addPart(FormBodyPartBuilder
                            .create("commands", new ByteArrayBody(commandsStream.toByteArray(), "commands"))
                            .build());
                } catch (IOException e) {
                    throw new RavenException("Unable to serialize the conversation attachment commands", e);
                }

                if (uniqueAttachmentStreams != null) {
                    int nameCounter = 1;

                    for (InputStream stream : uniqueAttachmentStreams) {
                        InputStreamBody inputStreamBody = new InputStreamBody(stream, (String) null);
                        FormBodyPart part = FormBodyPartBuilder.create("attachment" + nameCounter++, inputStreamBody)
                                .addField("Command-Type", "AttachmentStream")
                                .build();
                        entityBuilder.addPart(part);
                    }
                }

                request.setEntity(entityBuilder.build());
            }

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
}
