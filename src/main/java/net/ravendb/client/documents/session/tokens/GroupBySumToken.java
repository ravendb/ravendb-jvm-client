package net.ravendb.client.documents.session.tokens;

public class GroupBySumToken extends QueryToken {
    private final String _projectedName;
    private final String _fieldName;

    private GroupBySumToken(String fieldName, String projectedName) {
        if (fieldName == null) {
            throw new IllegalArgumentException("fieldName cannot be null");
        }

        this._fieldName = fieldName;
        this._projectedName = projectedName;
    }

    public static GroupBySumToken create(String fieldName, String projectedName) {
        return new GroupBySumToken(fieldName, projectedName);
    }

    @Override
    public void writeTo(StringBuilder writer) {
        writer
                .append("sum(")
                .append(_fieldName)
                .append(")");

        if (_projectedName == null) {
            return;
        }

        writer
                .append(" as ")
                .append(_projectedName);
    }
}
