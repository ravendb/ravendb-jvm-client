package net.ravendb.client.documents.operations.AI;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.operations.AI.agents.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

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
            throw new IllegalStateException("Conversation ID not set. Call run() first.");
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

    public void handle(String actionName, BiFunction<AiAgentActionRequest, Object, Object> action, AiHandleErrorStrategy strategy) {
        receive(actionName, (request, args) -> {
            Object result = action.apply(request, args);
            try {
                addActionResponse(request.getToolId(), result);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }, strategy);
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

    public <TAnswer> CompletableFuture<AiAnswer<TAnswer>> run() {
        return CompletableFuture.supplyAsync(() -> {
            while (true) {
                @SuppressWarnings("unchecked")
                AiAnswer<TAnswer> result = (AiAnswer<TAnswer>) runInternal().join();

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
                    }
                }

                if (this.actionResponses == null || this.actionResponses.isEmpty()) {
                    return result; // ActionsRequired, nothing to send back yet
                }
            }
        });
    }

    private <TAnswer> CompletableFuture<AiAnswer<TAnswer>> runInternal() {
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
                this.changeVector
        );

        return this.store.maintenance().forDatabase(this.databaseName).sendAsync(op)
                .thenApply(res -> {
                    ConversationResult<TAnswer> result = (ConversationResult<TAnswer>) res;

                    this.changeVector = result.getChangeVector();
                    this.conversationId = result.getConversationId();
                    this.actionRequests = result.getActionRequests() != null ? result.getActionRequests() : new ArrayList<>();

                    AiAnswer<TAnswer> answer = new AiAnswer<>();
                    answer.setAnswer(result.getResponse());
                    answer.setStatus(this.actionRequests.isEmpty() ? AiConversationResult.Done : AiConversationResult.ActionRequired);

                    return answer;
                })
                .whenComplete((r, ex) -> {
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
            pad.append("  "); // two spaces per level
        }
        return pad.toString();
    }
}
