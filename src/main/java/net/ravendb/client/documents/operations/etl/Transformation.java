package net.ravendb.client.documents.operations.etl;

import java.util.ArrayList;
import java.util.List;

public class Transformation {
    private String name;
    private boolean disabled;
    private List<String> collections = new ArrayList<>();
    private boolean applyToAllDocuments;
    private String script;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isDisabled() {
        return disabled;
    }

    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
    }

    public List<String> getCollections() {
        return collections;
    }

    public void setCollections(List<String> collections) {
        this.collections = collections;
    }

    public boolean isApplyToAllDocuments() {
        return applyToAllDocuments;
    }

    public void setApplyToAllDocuments(boolean applyToAllDocuments) {
        this.applyToAllDocuments = applyToAllDocuments;
    }

    public String getScript() {
        return script;
    }

    public void setScript(String script) {
        this.script = script;
    }
}
