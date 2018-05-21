package net.ravendb.client.documents.operations;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
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
     * @return Patch script
     */
    public String getScript() {
        return script;
    }

    /**
     * JavaScript function to use to patch a document
     * @param script Sets the value
     */
    public void setScript(String script) {
        this.script = script;
    }

    /**
     * Additional arguments passed to JavaScript function from Script.
     * @return additional arguments
     */
    public Map<String, Object> getValues() {
        return values;
    }

    /**
     * Additional arguments passed to JavaScript function from Script.
     * @param values Sets patch arguments
     */
    public void setValues(Map<String, Object> values) {
        this.values = values;
    }

    public PatchRequest() {
        values = new HashMap<>();
    }

    public static PatchRequest forScript(String script) {
        PatchRequest request = new PatchRequest();
        request
                .setScript(script);

        return request;
    }

    public void serialize(JsonGenerator generator, ObjectMapper entityMapper) throws IOException {
        generator.writeStartObject();
        generator.writeStringField("Script", script);
        generator.writeFieldName("Values");
        entityMapper.writeValue(generator, values);
        generator.writeEndObject();
    }
}
