package net.ravendb.client.documents.session.tokens;

public class CompareExchangeValueIncludesToken extends QueryToken {

    private final String _path;

    private CompareExchangeValueIncludesToken(String path) {
        if (path == null) {
            throw new IllegalArgumentException("Path cannot be null");
        }

        _path = path;
    }

    public static CompareExchangeValueIncludesToken create(String path) {
        return new CompareExchangeValueIncludesToken(path);
    }

    @Override
    public void writeTo(StringBuilder writer) {
        writer
                .append("cmpxchg('")
                .append(_path)
                .append("')");
    }
}
