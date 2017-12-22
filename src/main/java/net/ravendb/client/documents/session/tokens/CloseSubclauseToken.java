package net.ravendb.client.documents.session.tokens;

public class CloseSubclauseToken extends QueryToken {
    private CloseSubclauseToken() {
    }

    public static final CloseSubclauseToken INSTANCE = new CloseSubclauseToken();

    @Override
    public void writeTo(StringBuilder writer) {
        writer
                .append(")");
    }
}
