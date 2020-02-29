package net.ravendb.client.documents.indexes.mapReduce;

import java.util.List;

public class OutputReduceToCollectionReference {
    private String id;
    private List<String> reduceOutputs;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public List<String> getReduceOutputs() {
        return reduceOutputs;
    }

    public void setReduceOutputs(List<String> reduceOutputs) {
        this.reduceOutputs = reduceOutputs;
    }
}
