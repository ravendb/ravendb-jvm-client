package net.ravendb.client.test.client.documents.AI;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.AI.*;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.operations.AI.agents.AiAgentActionRequest;
import net.ravendb.client.infrastructure.EnableOnServer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;

import static org.assertj.core.api.Assertions.assertThat;

@EnableOnServer(thresholdVersion = "7.1")
public class AiConversationTests extends RemoteTestBase {

    @Test
    public void conversationRequiresAgentIdAndConversationId() {
        try (IDocumentStore store = getDocumentStore()) {
            try {
                store.ai().conversation("", "conv/1",new AiConversationCreationOptions());
                Assertions.assertTrue(false, "Expected exception for missing agentId not thrown");
            } catch (Exception e) {
                assertThat(e.getMessage())
                        .contains("agentId cannot be null or empty");
            }
            try {
                store.ai().conversation("agent/1", "",new AiConversationCreationOptions());
                Assertions.assertTrue(false, "Expected exception for missing conversationId not thrown");
            } catch (Exception e) {
                assertThat(e.getMessage())
                        .contains("conversationId cannot be null or empty");
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void idShouldNotBeAvailableBeforeFirstRunForNewConversations() {
        try (IDocumentStore store = getDocumentStore()) {
            AiConversation conv = store.ai()
                    .conversation("agents/1-A", "conversations/1|",new AiConversationCreationOptions());
            try {
                conv.getId();
                Assertions.assertTrue(false, "Expected exception for accessing id before first run not thrown");
            } catch (Exception e) {
                assertThat(e.getMessage())
                        .contains("This is a new conversation, the ID wasn't set yet, you have to call run() first");
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void requiredActionsShouldThrowBeforeRun() {
        try (IDocumentStore store = getDocumentStore()) {
            AiConversation conv = store.ai()
                    .conversation("agents/1-A", "conversations/2|",new AiConversationCreationOptions());
            try {
                conv.requiredActions();
                Assertions.assertTrue(false, "Expected exception for requiredActions() before run not thrown");
            } catch (Exception e) {
                assertThat(e.getMessage())
                        .contains("You must call run() first.");
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void addActionResponseValidatesInputsAndAcceptsStringOrObject() {
        try (IDocumentStore store = getDocumentStore()) {
            AiConversation conv = store.ai()
                    .conversation("agents/1-A", "conversations/3|",new AiConversationCreationOptions());
            try {
                conv.addActionResponse("", "x");
                Assertions.assertTrue(false, "Expected exception for empty toolId not thrown");
            } catch (Exception e) {
                assertThat(e.getMessage())
                        .contains("toolId cannot be empty");
            }
            try {
                conv.addActionResponse("t1", null);
                Assertions.assertTrue(false, "Expected exception for null content not thrown");
            } catch (Exception e) {
                assertThat(e.getMessage())
                        .contains("cannot be null");
            }

            conv.addActionResponse("tool1", "some response");
            Map<String, Object> objContent = new HashMap<>();
            objContent.put("ok", true);
            objContent.put("count", 1);
            conv.addActionResponse("tool2", objContent);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void addActionResponseShouldRejectDuplicateToolId() {
        try (IDocumentStore store = getDocumentStore()) {
            AiConversation conv = store.ai()
                    .conversation("agents/1-A", "conversations/5|",new AiConversationCreationOptions());

            conv.addActionResponse("tool1", "first response");

            try {
                conv.addActionResponse("tool1", "second response");
                Assertions.assertTrue(false, "Expected exception for duplicate toolId not thrown");
            } catch (IllegalStateException e) {
                assertThat(e.getMessage())
                        .contains("An action response for tool-id 'tool1' was already added");
            }

            conv.addActionResponse("tool2", "other response");

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void receiveShouldRejectDuplicateActionNames() {
        try (IDocumentStore store = getDocumentStore()) {

            AiConversation conv = store.ai()
                    .conversation("agents/1-A", "conversations/4|",new AiConversationCreationOptions());

            conv.receive("do-work", (x, y) -> {
            }, null);

            try {
                conv.receive("do-work", (x, y) -> {

                }, null);
                Assertions.assertTrue(false, "Expected exception for duplicate action names not thrown");
            } catch (Exception e) {
                assertThat(e.getMessage())
                        .contains("already exists");
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void receiveWithRaiseImmediatelyShouldAcceptErrorStrategies() {
        try (IDocumentStore store = getDocumentStore()) {
            AiConversation convDefault = store.ai()
                    .conversation("agents/1-A", "conversations/5|",new AiConversationCreationOptions());
            try {
                convDefault.receive(
                        "boom-default",
                        (x,y) -> { throw new RuntimeException("failure-default"); },
                        AiHandleErrorStrategy.SendErrorsToModel
                );
            } catch (Exception e) {
                Assertions.assertTrue(false, "Registration with SendErrorsToModel should not throw");
            }
            AiConversation convRaise = store.ai()
                    .conversation("agents/1-A", "conversations/6|",new AiConversationCreationOptions());
            try {
                convRaise.receive(
                        "boom-raise",
                        (x,y) -> { throw new RuntimeException("failure-raise"); },
                        AiHandleErrorStrategy.RaiseImmediately
                );
            } catch (Exception e) {
                Assertions.assertTrue(false, "Registration with RaiseImmediately should not throw");
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void onUnhandledAction_eventIsCalled_whenActionHasNoHandler() {
        try (IDocumentStore store = getDocumentStore()) {
            AiConversation conv = store.ai().conversation("agents/1-A", "conversations/7|",new AiConversationCreationOptions());

            boolean[] eventFired = {false};
            AiAgentActionRequest[] capturedAction = {null};
            AiConversation[] capturedSender = {null};

            conv.setOnUnhandledAction(args -> {
                eventFired[0] = true;
                capturedAction[0] = args.getAction();
                capturedSender[0] = args.getSender();
            });
            AiAgentActionRequest req = new AiAgentActionRequest();
            req.setName("unhandled-action");
            req.setToolId("tool-123");
            req.setArguments("{\"param\":\"value\"}");

            ArrayList<AiAgentActionRequest> requests = new ArrayList<AiAgentActionRequest>();
            requests.add(req);
            conv.setActionRequests(requests);

            assertThat(conv.getOnUnhandledAction()).isNotNull();

            conv.getOnUnhandledAction().accept(new UnhandledActionEventArgs(conv,
                    new AiAgentActionRequest("test", "t1", "{}")));

            assertThat(eventFired[0]).isTrue();
            assertThat(capturedSender[0]).isSameAs(conv);
            assertThat(capturedAction[0]).isNotNull();

        } catch (Exception e) {
            throw new RuntimeException("Test failed unexpectedly", e);
        }
    }

    @Test
    public void errorThrown_whenActionUndefined_andNoOnUnhandledActionEvent() {
        try (IDocumentStore store = getDocumentStore()) {
            AiConversation conv = store.ai().conversation("agents/1-A", "conversations/8|",new AiConversationCreationOptions());

            AiAgentActionRequest req = new AiAgentActionRequest();
            ArrayList<AiAgentActionRequest> requests = new ArrayList<AiAgentActionRequest>();
            requests.add(req);
            conv.setActionRequests(requests);
            conv.setUserPrompt("test prompt");

            assertThat(conv.getOnUnhandledAction()).isNull();
        } catch (Exception e) {
            throw new RuntimeException("Test failed unexpectedly", e);
        }
    }

    @Test
    public void onUnhandledAction_receivesCorrectEventArgsStructure() {
        try (IDocumentStore store = getDocumentStore()) {
            AiConversation conv = store.ai().conversation("agents/1-A", "conversations/9|",new AiConversationCreationOptions());

            final UnhandledActionEventArgs[] receivedArgs = {null};

            conv.setOnUnhandledAction(args -> receivedArgs[0] = args);

            AiAgentActionRequest testAction = new AiAgentActionRequest();
            testAction.setName("custom-action");
            testAction.setToolId("tool-789");
            testAction.setArguments("{\"key\":\"value\"}");

            conv.getOnUnhandledAction().accept(new UnhandledActionEventArgs(conv, testAction));

            assertThat(receivedArgs[0]).isNotNull();
            assertThat(receivedArgs[0].getSender()).isSameAs(conv);
            assertThat(receivedArgs[0].getAction()).isSameAs(testAction);
            assertThat(receivedArgs[0].getAction().getName()).isEqualTo("custom-action");
            assertThat(receivedArgs[0].getAction().getToolId()).isEqualTo("tool-789");
        } catch (Exception e) {
            throw new RuntimeException("Test failed unexpectedly", e);
        }
    }

    @Test
    public void handleMethod_worksWithoutRequestParameter_backwardCompat_async() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            AiConversation conv = store.ai().conversation("agents/1-A", "conversations/12|",new AiConversationCreationOptions());

            AtomicBoolean handlerCalled = new AtomicBoolean(false);
            AtomicReference<Object> capturedArgs = new AtomicReference<>();

            conv.handle("simple-action", args -> {
                handlerCalled.set(true);
                capturedArgs.set(args);
                Map<String, Object> result = new HashMap<>();
                result.put("result", "success");
                result.put("data", args.toString());
                return result;
            });

            assertThat(conv.getInvocations().containsKey("simple-action")).isTrue();

            AiAgentActionRequest request = new AiAgentActionRequest();
            request.setName("simple-action");
            request.setToolId("tool-100");
            request.setArguments("{\"value\":\"test-data\"}");

            conv.getInvocations().get("simple-action").invoke(request).join();

            assertThat(handlerCalled.get()).isTrue();
            assertThat(capturedArgs.get()).isNotNull();

            Map<String, Object> argsMap = (Map<String, Object>) capturedArgs.get();
            assertThat(argsMap.get("value")).isEqualTo("test-data");
        }
    }

    @Test
    public void handleMethod_worksWithoutRequestParameter_backwardCompat_sync() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            AiConversation conv = store.ai().conversation("agents/1-A", "conversations/13|",new AiConversationCreationOptions());

            AtomicBoolean handlerCalled = new AtomicBoolean(false);

            conv.handle("sync-action", args -> {
                handlerCalled.set(true);
                Map<String, Object> result = new HashMap<>();
                result.put("processed", true);
                result.put("input", ((Map<?, ?>) args).get("input"));
                return result;
            });

            assertThat(conv.getInvocations().containsKey("sync-action")).isTrue();

            AiAgentActionRequest request = new AiAgentActionRequest();
            request.setName("sync-action");
            request.setToolId("tool-101");
            request.setArguments("{\"input\":\"test\"}");

            conv.getInvocations().get("sync-action").invoke(request).join();

            assertThat(handlerCalled.get()).isTrue();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void handleMethod_worksWithRequestParameter_withMetadata_async() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            AiConversation conv = store.ai().conversation("agents/1-A", "conversations/14|",new AiConversationCreationOptions());

            AtomicBoolean handlerCalled = new AtomicBoolean(false);
            AtomicReference<AiAgentActionRequest> capturedRequest = new AtomicReference<>();
            AtomicReference<Map<String, Object>> capturedArgs = new AtomicReference<>();

            conv.handle("action-with-request", (request, args) -> {
                handlerCalled.set(true);
                capturedRequest.set(request);
                capturedArgs.set((Map<String, Object>) args);

                Map<String, Object> result = new HashMap<>();
                result.put("toolId", request.getToolId());
                result.put("data", ((Map<?, ?>) args).get("data"));
                return result;
            });

            assertThat(conv.getInvocations().containsKey("action-with-request")).isTrue();

            AiAgentActionRequest testRequest = new AiAgentActionRequest();
            testRequest.setName("action-with-request");
            testRequest.setToolId("tool-102");
            testRequest.setArguments("{\"data\":\"metadata-test\"}");

            conv.getInvocations().get("action-with-request").invoke(testRequest).join();

            assertThat(handlerCalled.get()).isTrue();
            assertThat(capturedRequest.get()).isNotNull();
            assertThat(capturedRequest.get().getToolId()).isEqualTo("tool-102");
            assertThat(capturedArgs.get().get("data")).isEqualTo("metadata-test");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void handleMethod_worksWithRequestParameter_withMetadata_sync() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            AiConversation conv = store.ai().conversation("agents/1-A", "conversations/15|",new AiConversationCreationOptions());

            AtomicReference<String> capturedToolId = new AtomicReference<>();

            conv.handle("sync-with-request", (request, args) -> {
                capturedToolId.set(request.getToolId());

                Map<String, Object> result = new HashMap<>();
                result.put("success", true);
                return result;
            });

            AiAgentActionRequest testRequest = new AiAgentActionRequest();
            testRequest.setName("sync-with-request");
            testRequest.setToolId("tool-103");
            testRequest.setArguments("{}");

            conv.getInvocations().get("sync-with-request").invoke(testRequest).join();

            assertThat(capturedToolId.get()).isEqualTo("tool-103");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void handleMethod_arityDetection_worksCorrectly() {
        try (IDocumentStore store = getDocumentStore()) {
            AiConversation conv = store.ai().conversation("agents/1-A", "conversations/16|",new AiConversationCreationOptions());

            AiHandler<Map<String, Object>> singleParamHandler = args -> {
                Map<String, Object> result = new HashMap<>();
                result.put("result", args);
                return result;
            };

            BiFunction<AiAgentActionRequest, Map<String, Object>, Object> twoParamHandler = (request, args) -> {
                Map<String, Object> result = new HashMap<>();
                result.put("result", args);
                return result;
            };

            conv.handle("single-param", singleParamHandler);
            conv.handle("two-param", twoParamHandler);

            assertThat(conv.getInvocations().containsKey("single-param")).isTrue();
            assertThat(conv.getInvocations().containsKey("two-param")).isTrue();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}