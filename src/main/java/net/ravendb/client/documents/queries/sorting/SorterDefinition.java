package net.ravendb.client.documents.queries.sorting;

public class SorterDefinition {
    /**
     * Name of the sorter
     */
    private String name;
    /**
     * C# source-code of the sorter
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
