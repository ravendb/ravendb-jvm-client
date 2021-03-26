package net.ravendb.client.documents.session.tokens;

public class OpenSubclauseToken extends QueryToken {
    private OpenSubclauseToken() {
    }

    public static OpenSubclauseToken create() {
        return new OpenSubclauseToken();
    }


    @Override
    public void writeTo(StringBuilder writer) {

        writer
                .append("(");
    }
}
