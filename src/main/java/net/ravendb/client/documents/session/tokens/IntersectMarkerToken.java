package net.ravendb.client.documents.session.tokens;

public class IntersectMarkerToken extends QueryToken {

    private IntersectMarkerToken() {
    }

    public static final IntersectMarkerToken INSTANCE = new IntersectMarkerToken();

    @Override
    public void writeTo(StringBuilder writer) {
        writer
                .append(",");
    }
}
