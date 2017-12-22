package net.ravendb.client.documents.session.tokens;

public class TrueToken extends QueryToken {

    private TrueToken() {
    }

    public static final TrueToken INSTANCE = new TrueToken();

    @Override
    public void writeTo(StringBuilder writer) {
        writer
                .append("true");
    }
}
