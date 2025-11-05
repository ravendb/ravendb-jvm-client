package net.ravendb.client.documents.AI;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.node.ObjectNode;

public abstract class ContentPart {

    @JsonProperty("type")
    private final String type;

    protected ContentPart(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }

    public abstract ObjectNode toJson();
}
