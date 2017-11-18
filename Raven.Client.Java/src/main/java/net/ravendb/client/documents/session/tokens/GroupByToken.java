package net.ravendb.client.documents.session.tokens;

public class GroupByToken extends QueryToken {

    private final String _fieldName;

    private GroupByToken(String fieldName) {
        _fieldName = fieldName;
    }

    public static GroupByToken create(String fieldName) {
        return new GroupByToken(fieldName);
    }

    @Override
    public void writeTo(StringBuilder writer) {
        writeField(writer, _fieldName);
    }
}
