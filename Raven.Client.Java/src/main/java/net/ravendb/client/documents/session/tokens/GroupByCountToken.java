package net.ravendb.client.documents.session.tokens;

public class GroupByCountToken extends QueryToken {

    private final String _fieldName;


    private GroupByCountToken(String fieldName) {
        _fieldName = fieldName;
    }

    public static GroupByCountToken create(String fieldName) {
        return new GroupByCountToken(fieldName);
    }

    @Override
    public void writeTo(StringBuilder writer) {
        writer
                .append("count()");

        if (_fieldName == null) {
            return;
        }

        writer
                .append(" as ")
                .append(_fieldName);
    }
}
