package net.ravendb.client.documents.session.tokens;

public class TimingsToken extends QueryToken {

    private TimingsToken() {
    }

    public static final TimingsToken INSTANCE = new TimingsToken();

    @Override
    public void writeTo(StringBuilder writer) {
        writer.append("timings()");
    }
}
