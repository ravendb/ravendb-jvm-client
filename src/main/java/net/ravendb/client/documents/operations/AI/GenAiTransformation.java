package net.ravendb.client.documents.operations.AI;

import net.ravendb.client.primitives.Reference;

public class GenAiTransformation {

    private String script;

    public String getScript() {
        return script;
    }

    public void setScript(String script) {
        this.script = script;
    }

    /**
     * Validates that the script calls ai.genContext(ctx).
     * @param error receives an explanatory error message when the script is invalid (empty otherwise)
     * @return true if the script is valid
     */
    public boolean validateScript(Reference<String> error) {
        error.value = "";
        if (script.contains("ai.genContext")) {
            return true;
        }
        error.value = "You must call the ai.genContext(ctx) function in your script";
        return false;
    }
}
