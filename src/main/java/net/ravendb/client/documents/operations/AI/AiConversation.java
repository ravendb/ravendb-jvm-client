package net.ravendb.client.documents.operations.AI;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.operations.AI.agents.*;
import org.apache.commons.lang3.StringUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;

public class AiConversation {
    private final IDocumentStore store;
    private final String databaseName;
    private final String agentId;
    private String conversationId;
    private final AiConversationCreationOptions options;
    private String changeVector;
    private List<AiAgentActionRequest> actionRequests = null;
    private final List<AiAgentActionResponse> actionResponses = new ArrayList<>();
    private String userPrompt;
    private final Map<String, IActionInvocation> invocations = new HashMap<>();

    private Consumer<UnhandledActionEventArgs> onUnhandledAction;

    public List<AiAgentActionResponse> getActionResponses() { return actionResponses; }
    public void setActionResponses(List<AiAgentActionResponse> actionResponses) { this.actionResponses.addAll(actionResponses); }

    public Map<String, IActionInvocation> getInvocations() { return invocations; }
    public void setInvocations(Map<String, IActionInvocation> invocations) { this.invocations.putAll(invocations); }

    public Consumer<UnhandledActionEventArgs> getOnUnhandledAction() { return onUnhandledAction; }
    public void setOnUnhandledAction(Consumer<UnhandledActionEventArgs> onUnhandledAction) { this.onUnhandledAction = onUnhandledAction; }

    public void setActionRequests(List<AiAgentActionRequest> actionRequests) {
        this.actionRequests = actionRequests;
    }
    public List<AiAgentActionRequest> getActionRequests() {return actionRequests; }

    public AiConversation(IDocumentStore store, String databaseName, String agentId, String conversationId,
                          AiConversationCreationOptions options, String changeVector) {
        if (store == null) throw new IllegalArgumentException("store is required");
        if (databaseName == null || databaseName.isEmpty()) throw new IllegalArgumentException("databaseName is required");
        if (agentId == null || agentId.isEmpty()) throw new IllegalArgumentException("agentId is required");
        if (conversationId == null || conversationId.isEmpty()) throw new IllegalArgumentException("conversationId is required");

        this.store = store;
        this.databaseName = databaseName;
        this.agentId = agentId;
        this.conversationId = conversationId;
        this.options = options;
        this.changeVector = changeVector;
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
                : new ObjectMapper().writeValueAsString(actionResponse); // Jackson serialization

        actionResponses.add(new AiAgentActionResponse(toolId, content));
    }

    public void setUserPrompt(String userPrompt) {
        if (userPrompt == null || userPrompt.isEmpty()) throw new IllegalArgumentException("userPrompt cannot be empty");
        this.userPrompt = userPrompt;
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

        CompletableFuture<AiAnswer<TAnswer>> future = new CompletableFuture<>();

        CompletableFuture.runAsync(() -> {
            while (true) {
                @SuppressWarnings("unchecked")
                AiAnswer<TAnswer> result;
                try {
                    result = (AiAnswer<TAnswer>) runInternal(streamPropertyPath, streamCallback).join();
                } catch (Exception e) {
                    future.completeExceptionally(e);
                    return;
                }

                if ("Done".equals(result.getStatus())) {
                    future.complete(result);
                    return;
                }

                if (actionRequests == null || actionRequests.isEmpty()) {
                    future.completeExceptionally(new IllegalStateException(
                            "There are no action requests to process, but Status was " + result.getStatus() + ", should not be possible."));
                    return;
                }

                for (AiAgentActionRequest action : actionRequests) {
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
        return CompletableFuture.supplyAsync(() -> {
            while (true) {
                @SuppressWarnings("unchecked")
                AiAnswer<TAnswer> result = (AiAnswer<TAnswer>) runInternal(null, null).join();

                if ("Done".equals(result.getStatus())) {
                    return result;
                }

                if (this.actionRequests == null || this.actionRequests.isEmpty()) {
                    throw new IllegalStateException("There are no action requests to process, but Status was " + result.getStatus() + ", should not be possible.");
                }

                for (AiAgentActionRequest action : this.actionRequests) {
                    IActionInvocation invocation = this.invocations.get(action.getName());
                    if (invocation != null) {
                        try {
                            invocation.invoke(action).join(); // wait for invocation to complete
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

    private <TAnswer> CompletableFuture<AiAnswer<TAnswer>> runInternal(String streamPropertyPath, AiStreamCallback streamCallback) {
        if (this.actionRequests != null && this.userPrompt == null && this.actionResponses.isEmpty()) {
            AiAnswer<TAnswer> doneAnswer = new AiAnswer<>();
            doneAnswer.setStatus(AiConversationResult.Done);
            return CompletableFuture.completedFuture(doneAnswer);
        }

        RunConversationOperation<TAnswer> op = new RunConversationOperation<>(
                this.agentId,
                this.conversationId,
                this.userPrompt,
                this.actionResponses,
                this.options,
                this.changeVector,
                streamPropertyPath,
                streamCallback
        );

        CompletableFuture<ConversationResult<TAnswer>> rawFuture = this.store.maintenance()
                .forDatabase(this.databaseName)
                .sendAsync(op);

        return rawFuture.thenApply(res -> {
            @SuppressWarnings("unchecked")
            ConversationResult<TAnswer> result = (ConversationResult<TAnswer>) res;

            this.changeVector = result.getChangeVector();
            this.conversationId = result.getConversationId();
            this.actionRequests = result.getActionRequests() != null ? result.getActionRequests() : new ArrayList<>();

            AiAnswer<TAnswer> answer = new AiAnswer<>();
            answer.setAnswer(result.getResponse());
            answer.setStatus(this.actionRequests.isEmpty()
                    ? AiConversationResult.Done
                    : AiConversationResult.ActionRequired);

            return answer;
        }).whenComplete((r, ex) -> {
            this.userPrompt = null;
            this.actionResponses.clear();
        });

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
