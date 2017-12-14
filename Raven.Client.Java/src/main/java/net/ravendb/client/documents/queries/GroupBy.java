package net.ravendb.client.documents.queries;

public class GroupBy {

    private String field;
    private GroupByMethod method;

    private GroupBy() {
        // empty
    }

    public String getField() {
        return field;
    }

    public GroupByMethod getMethod() {
        return method;
    }

    public static GroupBy field(String fieldName) {
        GroupBy groupBy = new GroupBy();
        groupBy.field = fieldName;
        groupBy.method = GroupByMethod.NONE;

        return groupBy;
    }

    public static GroupBy array(String fieldName) {
        GroupBy groupBy = new GroupBy();
        groupBy.field = fieldName;
        groupBy.method = GroupByMethod.ARRAY;
        return groupBy;

    }
}
