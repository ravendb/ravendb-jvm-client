package net.ravendb.client.documents.session.tokens;

public class GraphQueryToken extends QueryToken {
    private String _query;

    public GraphQueryToken(String query) {
        _query = query;
    }

    @Override
    public void writeTo(StringBuilder writer) {
        writer.append(_query);
    }
}
