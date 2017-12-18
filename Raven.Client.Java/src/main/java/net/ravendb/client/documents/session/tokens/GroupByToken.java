package net.ravendb.client.documents.session.tokens;

import net.ravendb.client.documents.queries.GroupByMethod;

public class GroupByToken extends QueryToken {

    private final String _fieldName;
    private final GroupByMethod _method;

    private GroupByToken(String fieldName, GroupByMethod method) {
        _fieldName = fieldName;
        _method = method;
    }

    public static GroupByToken create(String fieldName) {
        return create(fieldName, GroupByMethod.NONE);
    }

    public static GroupByToken create(String fieldName, GroupByMethod method) {
        return new GroupByToken(fieldName, method);
    }

    @Override
    public void writeTo(StringBuilder writer) {
        if (_method != GroupByMethod.NONE) {
            writer.append("Array(");
        }
        writeField(writer, _fieldName);
        if (_method != GroupByMethod.NONE) {
            writer.append(")");
        }
    }
}
