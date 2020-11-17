package net.ravendb.client.documents.session.tokens;

public class OpenSubclauseToken extends QueryToken {
    private OpenSubclauseToken() {
    }

    private String boostParameterName;

    public static OpenSubclauseToken create() {
        return new OpenSubclauseToken();
    }

    public String getBoostParameterName() {
        return boostParameterName;
    }

    public void setBoostParameterName(String boostParameterName) {
        this.boostParameterName = boostParameterName;
    }

    @Override
    public void writeTo(StringBuilder writer) {
        if (boostParameterName != null) {
            writer.append("boost");
        }

        writer
                .append("(");
    }
}
