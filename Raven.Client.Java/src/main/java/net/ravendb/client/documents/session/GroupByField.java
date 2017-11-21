package net.ravendb.client.documents.session;

public class GroupByField {
    private String fieldName;
    private String projectedName;

    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    public String getProjectedName() {
        return projectedName;
    }

    public void setProjectedName(String projectedName) {
        this.projectedName = projectedName;
    }
}
