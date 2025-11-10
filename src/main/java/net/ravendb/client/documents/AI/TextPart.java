package net.ravendb.client.documents.AI;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import jakarta.validation.constraints.NotBlank;

public final class TextPart extends ContentPart {

    @NotBlank(message = "Text cannot be null or empty")
    @JsonProperty("text")
    private String text;

    public TextPart(String text) {
        super(AiMessagePrompt.AiMessagePromptTypes.TEXT);
        this.text = text;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    @Override
    public ObjectNode toJson() {
        ObjectNode json = JsonNodeFactory.instance.objectNode();
        json.put(AiMessagePrompt.AiMessagePromptFields.TYPE, getType());
        json.put(AiMessagePrompt.AiMessagePromptFields.TEXT, getText());
        return json;
    }
}

