package net.ravendb.client.documents.indexes;

public class IndexErrors {

    private String name;
    private IndexingError[] errors;

    public IndexErrors() {
        errors = new IndexingError[0];
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public IndexingError[] getErrors() {
        return errors;
    }

    public void setErrors(IndexingError[] errors) {
        this.errors = errors;
    }
}
