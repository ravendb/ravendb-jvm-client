package net.ravendb.client.documents.session.tokens;

public class OpenSubclauseToken extends QueryToken {
    private OpenSubclauseToken() {
    }

    public static final OpenSubclauseToken INSTANCE = new OpenSubclauseToken();

    @Override
    public void writeTo(StringBuilder writer) {
        writer
                .append("(");
    }
}
