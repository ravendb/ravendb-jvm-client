package net.ravendb.client.documents.operations.AI.agents;

import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.operations.IMaintenanceOperation;
import net.ravendb.client.http.RavenCommand;
import net.ravendb.client.http.ServerNode;
import net.ravendb.client.primitives.NetISO8601Utils;
import net.ravendb.client.primitives.SharpEnum;
import net.ravendb.client.util.UrlUtils;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;

import java.io.IOException;

/**
 * Reads messages from an AI agent conversation, with optional timestamp-based paging and view filtering.
 */
public class GetConversationMessagesOperation implements IMaintenanceOperation<AiConversationMessagesResult> {

    private final GetConversationMessagesOptions _parameters;

    public GetConversationMessagesOperation(String conversationId) {
        GetConversationMessagesOptions parameters = new GetConversationMessagesOptions();
        parameters.setConversationId(conversationId);

        parameters.validate();
        _parameters = parameters;
    }

    public GetConversationMessagesOperation(GetConversationMessagesOptions parameters) {
        if (parameters == null) {
            throw new IllegalArgumentException("parameters cannot be null");
        }

        parameters.validate();
        _parameters = parameters;
    }

    @Override
    public RavenCommand<AiConversationMessagesResult> getCommand(DocumentConventions conventions) {
        return new GetConversationMessagesCommand(_parameters);
    }

    private static class GetConversationMessagesCommand extends RavenCommand<AiConversationMessagesResult> {
        private final GetConversationMessagesOptions _params;

        public GetConversationMessagesCommand(GetConversationMessagesOptions parameters) {
            super(AiConversationMessagesResult.class);

            _params = parameters;
        }

        @Override
        public boolean isReadRequest() {
            return true;
        }

        @Override
        public HttpUriRequestBase createRequest(ServerNode node) {
            StringBuilder sb = new StringBuilder(node.getUrl())
                    .append("/databases/")
                    .append(node.getDatabase())
                    .append("/ai/agent/conversation/messages")
                    .append("?conversationId=").append(UrlUtils.escapeDataString(_params.getConversationId()));

            if (_params.getBefore() != null) {
                sb.append("&before=").append(UrlUtils.escapeDataString(NetISO8601Utils.format(_params.getBefore(), true)));
            }

            if (_params.getAfter() != null) {
                sb.append("&after=").append(UrlUtils.escapeDataString(NetISO8601Utils.format(_params.getAfter(), true)));
            }

            sb.append("&pageSize=").append(_params.getPageSize());
            sb.append("&detailLevel=").append(SharpEnum.value(_params.getDetailLevel()));

            return new HttpGet(sb.toString());
        }

        @Override
        public void setResponse(String response, boolean fromCache) throws IOException {
            if (response == null) {
                return; // 404 - conversation not found
            }

            result = mapper.readValue(response, resultClass);
        }
    }
}
