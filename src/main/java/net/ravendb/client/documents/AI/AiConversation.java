package net.ravendb.client.documents.AI;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.ravendb.client.documents.commands.batches.CopyAttachmentCommandData;
import net.ravendb.client.documents.commands.batches.ICommandData;
import net.ravendb.client.documents.commands.batches.PutAttachmentCommandData;
import net.ravendb.client.documents.operations.AI.agents.*;
import net.ravendb.client.exceptions.ConcurrencyException;
import net.ravendb.client.extensions.expressionExtension;
import net.ravendb.client.util.SerializableFunction;
import net.ravendb.client.util.ValidationMethods;
import org.apache.commons.lang3.StringUtils;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class AiConversation {
    private final AiOperations aiOperations;
    private final AiConversationCreationOptions options;
    private final String agentId;

    private String conversationId;
    private List<AiAgentActionRequest> actionRequests = null;
    private final Map<String, AiAgentActionResponse> actionResponses = new LinkedHashMap<>();
    private final List<ContentPart> promptParts = new ArrayList<>();
    private final List<AiAgentArtificialActionResponse> artificialActions = new ArrayList<>();
    private final List<ICommandData> attachmentsCommands = new ArrayList<>();
    private String changeVector;

    private final Map<String, IActionInvocation> invocations = new HashMap<>();
    private final Set<String> dispatchedToolIds = new HashSet<>();
    private Consumer<UnhandledActionEventArgs> onUnhandledAction;

    public Map<String, IActionInvocation> getInvocations() { return invocations; }
    public void setInvocations(Map<String, IActionInvocation> invocations) { this.invocations.putAll(invocations); }

    public Consumer<UnhandledActionEventArgs> getOnUnhandledAction() { return onUnhandledAction; }
    public void setOnUnhandledAction(Consumer<UnhandledActionEventArgs> onUnhandledAction) { this.onUnhandledAction = onUnhandledAction; }

    public void setActionRequests(List<AiAgentActionRequest> actionRequests) {
        this.actionRequests = actionRequests;
    }
    public List<AiAgentActionRequest> getActionRequests() {return actionRequests; }

    public AiConversation(AiOperations aiOperations, String agentId, String conversationId,
                          AiConversationCreationOptions options, String changeVector) {
        ValidationMethods.assertNotNullOrEmpty(aiOperations,"aiOperations");
        ValidationMethods.assertNotNullOrEmpty(agentId,"agentId");
        ValidationMethods.assertNotNullOrEmpty(conversationId, "conversationId");

        this.aiOperations = aiOperations;
        this.agentId = agentId;
        this.conversationId = conversationId;
        this.options = options;
        this.changeVector = changeVector;
    }

    /**
     * Adds a file attachment as a stream to the conversation turn.
     *
     * @param name the name of the attachment (e.g. "monthly_budget.pdf").
     *             A descriptive name is highly recommended as it helps the LLM understand the file's context and content.
     * @param stream the data stream of the file
     * @param contentType the MIME media type of the attachment content (e.g. image/png)
     */
    public void addAttachment(String name, InputStream stream, String contentType) {
        if (stream == null) {
            throw new IllegalArgumentException("stream cannot be null");
        }

        attachmentsCommands.add(new PutAttachmentCommandData("__this__", name, stream, contentType, null));
    }

    /**
     * Copies an existing attachment from a document in RavenDB into the conversation context.
     *
     * @param sourceDocumentId the ID of the document in RavenDB that contains the attachment
     * @param fileName the name to assign to the file in the conversation context
     */
    public void copyAttachmentFrom(String sourceDocumentId, String fileName) {
        ValidationMethods.assertNotNullOrEmpty(sourceDocumentId, "sourceDocumentId");
        ValidationMethods.assertNotNullOrEmpty(fileName, "fileName");

        attachmentsCommands.add(new CopyAttachmentCommandData(sourceDocumentId, fileName, "__this__", fileName, null));
    }

    public void addArtificialActionWithResponse(String toolId, String actionResponse) {
        ValidationMethods.assertNotNullOrEmpty(toolId, "toolId");
        ValidationMethods.assertNotNullOrEmpty(actionResponse, "actionResponse");

        artificialActions.add(new AiAgentArtificialActionResponse() {{
            toolId = toolId;
            content = actionResponse;
        }});
    }

    public <TResponse> void addArtificialActionWithResponse(String toolId, TResponse actionResponse) {
        ValidationMethods.assertNotNullOrEmpty(toolId, "toolId");

        if (actionResponse == null) {
            throw new IllegalArgumentException(
                    "Action response for '" + toolId + "' cannot be null."
            );
        }

        if (actionResponse instanceof String) {
            addArtificialActionWithResponse(toolId, (String) actionResponse);
            return;
        }

        try {
            ObjectMapper mapper = new ObjectMapper();
            String json = mapper.writeValueAsString(actionResponse);
            addArtificialActionWithResponse(toolId, json);
        } catch (Exception e) {
            throw new RuntimeException(
                    "Failed to serialize action response for '" + toolId + "'", e
            );
        }
    }

    public String getChangeVector() {
        return changeVector;
    }

    public String getId() {
        if (conversationId == null || conversationId.endsWith("/") || conversationId.endsWith("|")) {
            throw new IllegalStateException("This is a new conversation, the ID wasn't set yet, you have to call run() first");
        }
        return conversationId;
    }

    public List<AiAgentActionRequest> requiredActions() {
        if (actionRequests == null) {
            throw new IllegalStateException("You must call run() first.");
        }
        return actionRequests;
    }

    public void addActionResponse(String toolId, Object actionResponse) throws JsonProcessingException {
        if (toolId == null || toolId.isEmpty()) throw new IllegalArgumentException("toolId cannot be empty");
        if (actionResponse == null) throw new IllegalArgumentException("Action response cannot be null");

        String content = (actionResponse instanceof String)
                ? (String) actionResponse
                : new ObjectMapper().writeValueAsString(actionResponse);

        if (actionResponses.containsKey(toolId)) {
            throw new IllegalStateException("An action response for tool-id '" + toolId + "' was already added. " +
                    "Each tool call must have exactly one response. If you're using handle(), return the value " +
                    "from the handler (don't call addActionResponse() manually).");
        }

        actionResponses.put(toolId, new AiAgentActionResponse(toolId, content));
    }

    public void setUserPrompt(String userPrompt) {
        if (userPrompt == null || userPrompt.isEmpty()) throw new IllegalArgumentException("userPrompt cannot be empty");
        this.promptParts.clear();
        this.addUserPrompt(userPrompt);
    }

    public void addUserPrompt(String... prompts) {
        for (String prompt : prompts) {
            if (prompt == null || prompt.isEmpty()) {
                throw new IllegalArgumentException("prompt cannot be empty");
            }
            this.promptParts.add(new TextPart(prompt));
        }
    }

    public <TArgs> void handle(String actionName,
                       AiHandler<TArgs> handler) {
        handle(actionName, handler, AiHandleErrorStrategy.SendErrorsToModel);
    }

    public <TArgs> void handle(String actionName,
                               BiFunction<AiAgentActionRequest, TArgs, Object> handler) {
        handle(actionName, handler, AiHandleErrorStrategy.SendErrorsToModel);
    }

    public <TArgs> void handle(String actionName,
                               BiFunction<AiAgentActionRequest, TArgs, Object> handler,
                               AiHandleErrorStrategy strategy) {

        BiFunction<AiAgentActionRequest, TArgs, CompletableFuture<Object>> wrappedAction =
                (req, args) -> toFuture(handler.apply(req, args));

        receive(actionName, (request, args) -> {
            wrappedAction.apply(request, (TArgs) args).thenAccept(result -> {
                try {
                    addActionResponse(request.getToolId(), result);
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }
            });
        }, strategy);
    }

    @SuppressWarnings("unchecked")
    public <TArgs> void handle(String actionName,
                               AiHandler<TArgs> handler,
                               AiHandleErrorStrategy strategy) {

        BiFunction<AiAgentActionRequest, TArgs, CompletableFuture<Object>> wrappedAction =
                (req, args) -> toFuture(handler.invoke(args));

        receive(actionName, (request, args) -> {
            wrappedAction.apply(request, (TArgs) args).thenAccept(result -> {
                try {
                    addActionResponse(request.getToolId(), result);
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }
            });
        }, strategy);
    }

    /**
     * Registers a handler for an action tool that takes no arguments.
     * The handler's return value is sent to the model as the tool response.
     *
     * @param actionName the name of the action tool
     * @param handler supplies the tool response
     */
    public void handle(String actionName, Supplier<Object> handler) {
        handle(actionName, handler, AiHandleErrorStrategy.SendErrorsToModel);
    }

    /**
     * Registers a handler for an action tool that takes no arguments.
     * The handler's return value is sent to the model as the tool response.
     *
     * @param actionName the name of the action tool
     * @param handler supplies the tool response
     * @param strategy how errors thrown by the handler are handled
     */
    public void handle(String actionName, Supplier<Object> handler, AiHandleErrorStrategy strategy) {
        handle(actionName, args -> handler.get(), strategy);
    }

    private CompletableFuture<Object> toFuture(Object result) {
        if (result instanceof CompletableFuture) {
            return (CompletableFuture<Object>) result;
        }
        return CompletableFuture.completedFuture(result);
    }

    public void receive(String actionName, BiConsumer<AiAgentActionRequest, Object> action, AiHandleErrorStrategy strategy) {
        if (invocations.containsKey(actionName)) {
            throw new IllegalStateException("Action already exists: " + actionName);
        }

        IActionInvocation invocation = request -> {
            try {
                Object args = parseArgs(request.getArguments());
                action.accept(request, args);
                return CompletableFuture.completedFuture(null);
            } catch (Exception e) {
                if (strategy == AiHandleErrorStrategy.SendErrorsToModel) {
                    addActionResponse(request.getToolId(), createErrorMessageForLlm(e));
                    return CompletableFuture.completedFuture(null);
                } else {
                    throw e;
                }
            }
        };

        invocations.put(actionName, invocation);
    }

    /**
     * Registers a receiver for an action tool that takes no arguments.
     * The receiver is responsible for adding the tool response via {@link #addActionResponse(String, Object)}.
     *
     * @param actionName the name of the action tool
     * @param action receives the action request
     */
    public void receive(String actionName, Consumer<AiAgentActionRequest> action) {
        receive(actionName, action, AiHandleErrorStrategy.SendErrorsToModel);
    }

    /**
     * Registers a receiver for an action tool that takes no arguments.
     * The receiver is responsible for adding the tool response via {@link #addActionResponse(String, Object)}.
     *
     * @param actionName the name of the action tool
     * @param action receives the action request
     * @param strategy how errors thrown by the receiver are handled
     */
    public void receive(String actionName, Consumer<AiAgentActionRequest> action, AiHandleErrorStrategy strategy) {
        receive(actionName, (request, args) -> action.accept(request), strategy);
    }

    public <TAnswer> CompletableFuture<AiAnswer<TAnswer>> stream(SerializableFunction<TAnswer, ?> streamProperty, AiStreamCallback streamCallback) {
        return stream(expressionExtension.toPropertyPath(streamProperty,aiOperations.store.getConventions()), streamCallback);
    }

    public <TAnswer> CompletableFuture<AiAnswer<TAnswer>> stream(
            String streamPropertyPath,
            AiStreamCallback streamCallback
    ) {
        if (StringUtils.isBlank(streamPropertyPath)) {
            throw new IllegalArgumentException("streamPropertyPath cannot be empty");
        }
        if (streamCallback == null) {
            throw new IllegalArgumentException("streamCallback cannot be null");
        }

        return streamTurn(streamPropertyPath, streamCallback, null);
    }

    /**
     * Executes one turn of the conversation, streaming the specified property's value, with output format options
     * that override the agent's default output schema for this turn only.
     *
     * @param streamPropertyPath the property of the response to stream; not used when noSchema is set
     * @param streamCallback invoked with each streamed chunk
     * @param outputOptions options controlling the output format for this turn
     * @param <TAnswer> the expected type of the answer
     * @return the answer of this turn
     */
    public <TAnswer> CompletableFuture<AiAnswer<TAnswer>> streamWithSchema(String streamPropertyPath, AiStreamCallback streamCallback, AiOutputOptions outputOptions) {
        validateOutputOptions(outputOptions);
        if (!outputOptions.isNoSchema() && StringUtils.isBlank(streamPropertyPath)) {
            throw new IllegalArgumentException("streamPropertyPath cannot be empty");
        }
        if (streamCallback == null) {
            throw new IllegalArgumentException("streamCallback cannot be null");
        }

        return streamTurn(StringUtils.defaultString(streamPropertyPath), streamCallback, outputOptions);
    }

    /**
     * Executes one turn of the conversation, streaming the specified property's value, with output format options
     * that override the agent's default output schema for this turn only.
     *
     * @param streamProperty the property of the response to stream
     * @param streamCallback invoked with each streamed chunk
     * @param outputOptions options controlling the output format for this turn
     * @param <TAnswer> the expected type of the answer
     * @return the answer of this turn
     */
    public <TAnswer> CompletableFuture<AiAnswer<TAnswer>> streamWithSchema(SerializableFunction<TAnswer, ?> streamProperty, AiStreamCallback streamCallback, AiOutputOptions outputOptions) {
        return streamWithSchema(expressionExtension.toPropertyPath(streamProperty, aiOperations.store.getConventions()), streamCallback, outputOptions);
    }

    /**
     * Executes one turn of the conversation, streaming the specified property's value, deriving the output schema
     * from the sample object for this turn only.
     *
     * @param streamPropertyPath the property of the response to stream
     * @param streamCallback invoked with each streamed chunk
     * @param sampleObject a sample answer used to derive the output schema for this turn
     * @param <TAnswer> the expected type of the answer
     * @return the answer of this turn
     */
    public <TAnswer> CompletableFuture<AiAnswer<TAnswer>> streamWithSchema(String streamPropertyPath, AiStreamCallback streamCallback, TAnswer sampleObject) {
        return streamWithSchema(streamPropertyPath, streamCallback, new AiOutputOptions(sampleObject));
    }

    /**
     * Executes one turn of the conversation, streaming the specified property's value, deriving the output schema
     * from the sample object for this turn only.
     *
     * @param streamProperty the property of the response to stream
     * @param streamCallback invoked with each streamed chunk
     * @param sampleObject a sample answer used to derive the output schema for this turn
     * @param <TAnswer> the expected type of the answer
     * @return the answer of this turn
     */
    public <TAnswer> CompletableFuture<AiAnswer<TAnswer>> streamWithSchema(SerializableFunction<TAnswer, ?> streamProperty, AiStreamCallback streamCallback, TAnswer sampleObject) {
        return streamWithSchema(streamProperty, streamCallback, new AiOutputOptions(sampleObject));
    }

    /**
     * Executes one turn of the conversation without structured output, streaming the raw text response.
     *
     * @param streamCallback invoked with each streamed text chunk
     * @return the answer of this turn, as raw text
     */
    public CompletableFuture<AiAnswer<String>> streamWithoutSchema(AiStreamCallback streamCallback) {
        AiOutputOptions outputOptions = new AiOutputOptions();
        outputOptions.setNoSchema(true);
        return streamWithSchema("", streamCallback, outputOptions);
    }

    private <TAnswer> CompletableFuture<AiAnswer<TAnswer>> streamTurn(String streamPropertyPath, AiStreamCallback streamCallback, AiOutputOptions outputOptions) {
        CompletableFuture<AiAnswer<TAnswer>> future = new CompletableFuture<>();

        CompletableFuture.runAsync(() -> {
            while (true) {
                @SuppressWarnings("unchecked")
                AiAnswer<TAnswer> result;
                try {
                    result = (AiAnswer<TAnswer>) runInternal(streamPropertyPath, streamCallback, outputOptions).join();
                } catch (Exception e) {
                    future.completeExceptionally(e);
                    return;
                }

                if (result.getStatus() == AiConversationResult.Done) {
                    future.complete(result);
                    return;
                }

                if (actionRequests == null || actionRequests.isEmpty()) {
                    future.completeExceptionally(new IllegalStateException(
                            "There are no action requests to process, but Status was " + result.getStatus() + ", should not be possible."));
                    return;
                }

                for (AiAgentActionRequest action : actionRequests) {
                    if (!dispatchedToolIds.add(action.getToolId())) {
                        continue;
                    }

                    IActionInvocation invocation = invocations.get(action.getName());
                    try {
                        if (invocation != null) {
                            invocation.invoke(action).join();
                        } else if (onUnhandledAction != null) {
                            onUnhandledAction.accept(new UnhandledActionEventArgs(this, action));
                        } else {
                            throw new IllegalStateException(String.format(
                                    "There is no action defined for action '%s' on agent '%s' (%s), but it was invoked by the model with: %s. " +
                                            "Did you forget to call receive() or handle()? You can also handle unexpected action invocations using the onUnhandledAction event.",
                                    action.getName(),
                                    agentId,
                                    conversationId,
                                    action.getArguments()
                            ));
                        }
                    } catch (Exception e) {
                        future.completeExceptionally(e);
                        return;
                    }
                }

                if (actionResponses == null || actionResponses.isEmpty()) {
                    future.complete(result);
                    return;
                }
            }
        });

        return future;
    }

    public <TAnswer> CompletableFuture<AiAnswer<TAnswer>> run() {
        return runTurn(null);
    }

    /**
     * Executes one turn of the conversation with output format options that override the agent's default
     * output schema for this turn only, or disable structured output.
     *
     * @param outputOptions options controlling the output format for this turn
     * @param <TAnswer> the expected type of the answer
     * @return the answer of this turn
     */
    public <TAnswer> CompletableFuture<AiAnswer<TAnswer>> runWithSchema(AiOutputOptions outputOptions) {
        validateOutputOptions(outputOptions);
        return runTurn(outputOptions);
    }

    /**
     * Executes one turn of the conversation, deriving the output schema from the sample object for this turn only.
     *
     * @param sampleObject a sample answer used to generate the JSON schema sent to the LLM
     * @param <TAnswer> the expected type of the answer
     * @return the answer of this turn
     */
    public <TAnswer> CompletableFuture<AiAnswer<TAnswer>> runWithSchema(TAnswer sampleObject) {
        return runWithSchema(new AiOutputOptions(sampleObject));
    }

    /**
     * Executes one turn of the conversation with an explicit JSON schema override for this turn only.
     *
     * @param schema an explicit JSON schema to send to the LLM for this turn
     * @param <TAnswer> the expected type of the answer
     * @return the answer of this turn
     */
    public <TAnswer> CompletableFuture<AiAnswer<TAnswer>> runWithSchema(String schema) {
        return runWithSchema(new AiOutputOptions(schema));
    }

    /**
     * Executes one turn of the conversation without structured output, returning raw text.
     *
     * @return the answer of this turn, as raw text
     */
    public CompletableFuture<AiAnswer<String>> runWithoutSchema() {
        AiOutputOptions outputOptions = new AiOutputOptions();
        outputOptions.setNoSchema(true);
        return runWithSchema(outputOptions);
    }

    private <TAnswer> CompletableFuture<AiAnswer<TAnswer>> runTurn(AiOutputOptions outputOptions) {
        return CompletableFuture.supplyAsync(() -> {
            this.dispatchedToolIds.clear();

            while (true) {
                @SuppressWarnings("unchecked")
                AiAnswer<TAnswer> result = (AiAnswer<TAnswer>) runInternal(null, null, outputOptions).join();

                if (result.getStatus() == AiConversationResult.Done) {
                    return result;
                }

                if (this.actionRequests == null || this.actionRequests.isEmpty()) {
                    throw new IllegalStateException("There are no action requests to process, but Status was " + result.getStatus() + ", should not be possible.");
                }

                for (AiAgentActionRequest action : this.actionRequests) {
                    if (!this.dispatchedToolIds.add(action.getToolId())) {
                        continue;
                    }

                    IActionInvocation invocation = this.invocations.get(action.getName());
                    if (invocation != null) {
                        try {
                            invocation.invoke(action).join();
                        } catch (JsonProcessingException e) {
                            throw new RuntimeException(e);
                        }
                    } else if (this.onUnhandledAction != null) {
                        this.onUnhandledAction.accept(new UnhandledActionEventArgs(this, action));
                    } else {
                        throw new IllegalStateException(
                                String.format(
                                        "There is no action defined for action '%s' on agent '%s' (%s), but it was invoked by the model with: %s. " +
                                                "Did you forget to call receive() or handle()? You can also handle unexpected action invocations using the onUnhandledAction event.",
                                        action.getName(),
                                        this.agentId,
                                        this.conversationId,
                                        action.getArguments()
                                )
                        );
                    }
                }

                if (this.actionResponses == null || this.actionResponses.isEmpty()) {
                    return result;
                }
            }
        });
    }

    private <TAnswer> CompletableFuture<AiAnswer<TAnswer>> runInternal(String streamPropertyPath, AiStreamCallback streamCallback, AiOutputOptions outputOptions) {
        try {
            if (this.actionRequests != null && this.actionRequests.isEmpty() && this.promptParts.isEmpty() && this.actionResponses.isEmpty() && this.attachmentsCommands.isEmpty()) {
                AiAnswer<TAnswer> doneAnswer = new AiAnswer<>();
                doneAnswer.setStatus(AiConversationResult.Done);
                return CompletableFuture.completedFuture(doneAnswer);
            }

            RunConversationOperation<TAnswer> op = new RunConversationOperation<>(
                    this.agentId,
                    this.conversationId,
                    this.promptParts,
                    new ArrayList<>(this.actionResponses.values()),
                    this.artificialActions,
                    this.options,
                    this.changeVector,
                    new ArrayList<>(this.attachmentsCommands),
                    streamPropertyPath,
                    streamCallback,
                    outputOptions
            );

            ConversationResult<TAnswer> result = this.aiOperations.getExecutor().send(op);
            this.changeVector = result.getChangeVector();
            this.conversationId = result.getConversationId();
            this.actionRequests = result.getActionRequests() != null ? result.getActionRequests() : new ArrayList<>();

            AiAnswer<TAnswer> answer = new AiAnswer<>();
            answer.setAnswer(result.getResponse());
            answer.setStatus(result.getActionRequests() == null || result.getActionRequests().isEmpty()
                    ? AiConversationResult.Done
                    : AiConversationResult.ActionRequired);
            answer.setUsage(result.getUsage());
            answer.setElapsed(result.getElapsed());
            return CompletableFuture.completedFuture(answer);
        } catch (ConcurrencyException e) {
            this.changeVector = e.getActualChangeVector();
            throw e;
        }
        finally {
            this.promptParts.clear();
            this.actionResponses.clear();
            this.artificialActions.clear();
            this.attachmentsCommands.clear();
        }
    }

    private static void validateOutputOptions(AiOutputOptions outputOptions) {
        if (outputOptions == null) {
            throw new IllegalArgumentException("outputOptions cannot be null");
        }

        // the answer type is erased at runtime, so only the checks that don't depend on it can be done here
        if (outputOptions.isNoSchema() && (outputOptions.getOutputSchema() != null || outputOptions.getSampleObject() != null)) {
            throw new IllegalStateException("When noSchema is set to true you cannot set an output schema or a sample object, " +
                    "because the model is expected to return just a string, with no structure. " +
                    "You can either set noSchema to false, or remove the output schema and sample object.");
        }
    }

    private Object parseArgs(String argsJson) {
        try {
            return new ObjectMapper().readValue(argsJson, Object.class);
        } catch (Exception e) {
            return argsJson;
        }
    }

    private String createErrorMessageForLlm(Throwable e) {
        StringBuilder sb = new StringBuilder();
        int indent = 0;
        while (e != null) {
            String pad = getIndentation(indent);
            String name = e.getClass().getSimpleName();
            String msg = e.getMessage() != null ? e.getMessage() : e.toString();
            sb.append(pad).append(name).append(": ").append(msg).append("\n");
            e = e.getCause();
            indent++;
        }
        return sb.toString();
    }

    private String getIndentation(int level) {
        StringBuilder pad = new StringBuilder();
        for (int i = 0; i < level; i++) {
            pad.append("  ");
        }
        return pad.toString();
    }
}
