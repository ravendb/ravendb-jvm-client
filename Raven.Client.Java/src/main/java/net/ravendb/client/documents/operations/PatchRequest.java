package net.ravendb.client.documents.operations;

import java.util.HashMap;
import java.util.Map;

/**
 * An advanced patch request for a specified document (using JavaScript)
 */
public class PatchRequest {

    private String script;

    private Map<String, Object> values;

    /**
     * JavaScript function to use to patch a document
     */
    public String getScript() {
        return script;
    }

    /**
     * JavaScript function to use to patch a document
     */
    public void setScript(String script) {
        this.script = script;
    }

    /**
     * Additional arguments passed to JavaScript function from Script.
     */
    public Map<String, Object> getValues() {
        return values;
    }

    /**
     * Additional arguments passed to JavaScript function from Script.
     */
    public void setValues(Map<String, Object> values) {
        this.values = values;
    }

    public PatchRequest() {
        values = new HashMap<>();
    }
}
