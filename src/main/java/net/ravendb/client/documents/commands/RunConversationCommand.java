package net.ravendb.client.documents.commands;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.node.ObjectNode;
import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.operations.AI.agents.AiAgentActionResponse;
import net.ravendb.client.documents.operations.AI.agents.AiConversationCreationOptions;
import net.ravendb.client.documents.operations.AI.agents.ConversationResult;
import net.ravendb.client.http.IRaftCommand;
import net.ravendb.client.http.RavenCommand;
import net.ravendb.client.http.ServerNode;
import net.ravendb.client.json.ContentProviderHttpEntity;
import net.ravendb.client.util.RaftIdGenerator;
import net.ravendb.client.util.UrlUtils;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.core5.http.ContentType;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

public class RunConversationCommand<TAnswer>
        extends RavenCommand<ConversationResult<TAnswer>>
        implements IRaftCommand {

    private final String conversationId;
    private final String agentId;
    private final String prompt;
    private final List<AiAgentActionResponse> actionResponses;
    private final AiConversationCreationOptions options;
    private final String changeVector;
    private String raftId;

    public RunConversationCommand(
            String conversationId,
            String agentId,
            String prompt,
            List<AiAgentActionResponse> actionResponses,
            AiConversationCreationOptions options,
            String changeVector,
            DocumentConventions conventions) {
        super((Class<ConversationResult<TAnswer>>) (Class<?>) ConversationResult.class);
        this.conversationId = conversationId;
        this.agentId = agentId;
        this.prompt = prompt;
        this.actionResponses = actionResponses;
        this.options = options;
        this.changeVector = changeVector;

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
//                ObjectNode transformed = objectUtils.transformObjectKeys(
//                        bodyObj,
//                        objectUtils.pascalCase(),
//                        Collections.singletonList(Pattern.compile("^CreationOptions\\.Parameters\\..*$"))
//                );
//
//                generator.writeTree(transformed);
//            }
//        }, ContentType.APPLICATION_JSON, _conventions));
//
//        return request;
    }
}

