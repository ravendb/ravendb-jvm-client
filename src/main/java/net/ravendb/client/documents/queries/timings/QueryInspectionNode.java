package net.ravendb.client.documents.queries.timings;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class QueryInspectionNode {

    private String operation;
    private Map<String, String> parameters;
    private List<QueryInspectionNode> children;

    public QueryInspectionNode() {

    }

    public String getOperation() {
        return operation;
    }

    public void setOperation(String operation) {
        this.operation = operation;
    }

    public Map<String, String> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, String> parameters) {
        this.parameters = parameters;
    }

    public List<QueryInspectionNode> getChildren() {
        return children;
    }

    public void setChildren(List<QueryInspectionNode> children) {
        this.children = children;
    }

    public QueryInspectionNode cloneNode() {
        QueryInspectionNode cloned = new QueryInspectionNode();
        cloned.setOperation(this.operation);

        if (this.parameters != null) {
            cloned.setParameters(new HashMap<>());
            for (Map.Entry<String, String> entry : this.parameters.entrySet()) {
                cloned.getParameters().put(entry.getKey(), entry.getValue());
            }
        }

        if (this.children != null) {
            cloned.setChildren(new ArrayList<>());
            for (QueryInspectionNode child : this.children) {
                cloned.getChildren().add(child.cloneNode());
            }
        }

        return cloned;
    }
}
