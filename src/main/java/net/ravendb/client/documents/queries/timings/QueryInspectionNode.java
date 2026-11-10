package net.ravendb.client.documents.queries.timings;

import java.util.List;
import java.util.Map;

/**
 * Typed form of the Corax query plan returned in {@link QueryTimings#getQueryPlan()}.
 * Convert with {@code mapper.treeToValue(timings.getQueryPlan(), QueryInspectionNode.class)}.
 */
public class QueryInspectionNode {

    private String operation;
    private Map<String, String> parameters;
    private List<QueryInspectionNode> children;

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
}
