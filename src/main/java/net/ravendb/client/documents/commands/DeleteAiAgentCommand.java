package net.ravendb.client.documents.commands;

import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.operations.AI.agents.AiAgentConfigurationResult;
import net.ravendb.client.documents.operations.AI.agents.config.AiAgentConfiguration;
import net.ravendb.client.http.RavenCommand;
import net.ravendb.client.http.ServerNode;
import net.ravendb.client.util.UrlUtils;
import org.apache.hc.client5.http.classic.methods.HttpDelete;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;

public class DeleteAiAgentCommand extends RavenCommand<AiAgentConfigurationResult> {
    private final String identifier;
    private final DocumentConventions conventions;

    public DeleteAiAgentCommand(String identifier, DocumentConventions conventions) {
        super(AiAgentConfigurationResult.class);
        this.identifier = identifier;
        this.conventions = conventions;
    }

    @Override
    public boolean isReadRequest() {return false;}

    @Override
    public HttpUriRequestBase createRequest(ServerNode node){
        String uri = node.getUrl() + "/databases/" + node.getDatabase() + "/admin/ai/agent"
                + "?agentId=" + UrlUtils.escapeDataString(identifier);
        return new HttpDelete(uri);
    }
}
