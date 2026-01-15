package net.ravendb.client.documents.operations.AI.agents;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import net.ravendb.client.documents.AI.AiConversationCreationOptions;
import net.ravendb.client.documents.AI.ContentPart;

import java.util.List;

class ConversionRequestBody {

    private List<AiAgentActionResponse> actionResponses;
    private List<AiAgentArtificialActionResponse> artificialActions;
    private List<ContentPart> userPrompt;
    private AiConversationCreationOptions creationOptions;

    public List<AiAgentActionResponse> getActionResponses() {
        return actionResponses;
    }

    public void setActionResponses(List<AiAgentActionResponse> actionResponses) {
        this.actionResponses = actionResponses;
    }
    public List<AiAgentArtificialActionResponse> getArtificialActions() {
        return artificialActions;
    }

    public void setArtificialActions(List<AiAgentArtificialActionResponse> artificialActions) {
        this.artificialActions = artificialActions;
    }

    public List<ContentPart> getUserPrompt() {
        return userPrompt;
    }

    public void setUserPrompt(List<ContentPart> userPrompt) {
        this.userPrompt = userPrompt;
    }

    public AiConversationCreationOptions getCreationOptions() {
        return creationOptions;
    }

    public void setCreationOptions(AiConversationCreationOptions creationOptions) {
        this.creationOptions = creationOptions;
    }

    public ObjectNode toJson(ObjectMapper mapper) {
        ObjectNode json = mapper.createObjectNode();

        if (actionResponses != null) {
            ArrayNode arr = mapper.valueToTree(actionResponses);
            json.set("actionResponses", arr);
        } else {
            json.putNull("actionResponses");
        }

        json.set("creationOptions", mapper.valueToTree(
                creationOptions != null ? creationOptions : new AiConversationCreationOptions()
        ));

        if (userPrompt != null) {
            json.set("userPrompt", mapper.valueToTree(userPrompt));
        } else {
            json.putNull("userPrompt");
        }

        return json;
    }
}

