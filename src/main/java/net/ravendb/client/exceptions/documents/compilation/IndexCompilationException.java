package net.ravendb.client.exceptions.documents.compilation;

import net.ravendb.client.exceptions.compilation.CompilationException;

public class IndexCompilationException extends CompilationException {
    private String indexDefinitionProperty;
    private String problematicText;

    public IndexCompilationException() {
    }

    public IndexCompilationException(String message) {
        super(message);
    }

    public IndexCompilationException(String message, Throwable cause) {
        super(message, cause);
    }

    public String getIndexDefinitionProperty() {
        return indexDefinitionProperty;
    }

    public void setIndexDefinitionProperty(String indexDefinitionProperty) {
        this.indexDefinitionProperty = indexDefinitionProperty;
    }

    public String getProblematicText() {
        return problematicText;
    }

    public void setProblematicText(String problematicText) {
        this.problematicText = problematicText;
    }
}
