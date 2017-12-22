package net.ravendb.client.documents.session.tokens;

public class NegateToken extends QueryToken {
    private NegateToken() {
    }

    public static final NegateToken INSTANCE = new NegateToken();

    @Override
    public void writeTo(StringBuilder writer) {
        writer
                .append("not");
    }
}
