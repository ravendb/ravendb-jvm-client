package net.ravendb.client.exceptions.documents.compilation;

import net.ravendb.client.exceptions.compilation.CompilationException;

public class TransformerCompilationException extends CompilationException {
    private String transformerDefinitionProperty;
    private String problematicText;

    public TransformerCompilationException() {
    }

    public TransformerCompilationException(String message) {
        super(message);
    }

    public TransformerCompilationException(String message, Throwable cause) {
        super(message, cause);
    }

    public String getTransformerDefinitionProperty() {
        return transformerDefinitionProperty;
    }

    public void setTransformerDefinitionProperty(String transformerDefinitionProperty) {
        this.transformerDefinitionProperty = transformerDefinitionProperty;
    }

    public String getProblematicText() {
        return problematicText;
    }

    public void setProblematicText(String problematicText) {
        this.problematicText = problematicText;
    }
}
