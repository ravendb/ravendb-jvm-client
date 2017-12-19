package net.ravendb.client.documents.session.tokens;

public class DistinctToken extends QueryToken {

    private DistinctToken() {
    }

    public static final DistinctToken INSTANCE = new DistinctToken();

    @Override
    public void writeTo(StringBuilder writer) {
        writer
                .append("distinct");
    }
}
