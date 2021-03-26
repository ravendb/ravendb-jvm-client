package net.ravendb.client.documents.session.tokens;

public class CloseSubclauseToken extends QueryToken {
    private CloseSubclauseToken() {
    }


    public static CloseSubclauseToken create() {
        return new CloseSubclauseToken();
    }



    @Override
    public void writeTo(StringBuilder writer) {

        writer
                .append(")");
    }
}
