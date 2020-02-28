package net.ravendb.client.documents.session.tokens;

public class WithToken extends QueryToken {
    private final String _alias;
    private final String _query;

    public WithToken(String alias, String query) {
        _alias = alias;
        _query = query;
    }

    @Override
    public void writeTo(StringBuilder writer) {
        writer.append("with {");
        writer.append(_query);
        writer.append("} as ");
        writer.append(_alias);
    }
}
