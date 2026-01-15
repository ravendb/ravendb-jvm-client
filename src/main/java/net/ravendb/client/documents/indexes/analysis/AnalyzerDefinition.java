package net.ravendb.client.documents.indexes.analysis;

public class AnalyzerDefinition {
    /**
     * Name of the analyzer
     */
    private String name;
    /**
     * Code of the analyzer
     */
    private String code;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }
}
