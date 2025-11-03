package net.ravendb.client.documents.AI;

import com.fasterxml.jackson.core.JsonProcessingException;
import net.ravendb.client.documents.operations.AI.agents.AiAgentActionRequest;
import java.util.concurrent.CompletableFuture;

@FunctionalInterface
public interface IActionInvocation {
    CompletableFuture<Void> invoke(AiAgentActionRequest request) throws JsonProcessingException;
}
