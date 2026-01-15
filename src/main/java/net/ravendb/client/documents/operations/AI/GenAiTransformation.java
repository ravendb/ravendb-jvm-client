package net.ravendb.client.documents.operations.AI;

public class GenAiTransformation {

    private String script;

    public String getScript() {
        return script;
    }

    public void setScript(String script) {
        this.script = script;
    }

    public boolean validateScript(StringBuilder error) {
        if (script != null && script.contains("ai.genContext")) {
            error.setLength(0); // clear
            return true;
        }

        error.setLength(0);
        error.append("You must call the ai.genContext(ctx) function in your script");
        return false;
    }
}
