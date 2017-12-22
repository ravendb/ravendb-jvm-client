package net.ravendb.client.documents.session.tokens;

import org.apache.commons.lang3.ObjectUtils;

public class GroupByKeyToken extends QueryToken {

    private final String _fieldName;
    private final String _projectedName;

    private GroupByKeyToken(String fieldName, String projectedName) {
        _fieldName = fieldName;
        _projectedName = projectedName;
    }

    public static GroupByKeyToken create(String fieldName, String projectedName) {
        return new GroupByKeyToken(fieldName, projectedName);
    }

    @Override
    public void writeTo(StringBuilder writer) {
        writeField(writer, ObjectUtils.firstNonNull(_fieldName, "key()"));

        if (_projectedName == null || _projectedName.equals(_fieldName)) {
            return;
        }

        writer
                .append(" as ")
                .append(_projectedName);
    }
}
