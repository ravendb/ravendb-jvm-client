package net.ravendb.client.documents.session.tokens;

public class CloseSubclauseToken extends QueryToken {
    private CloseSubclauseToken() {
    }

    private String boostParameterName;

    public static CloseSubclauseToken create() {
        return new CloseSubclauseToken();
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
            writer.append(", $").append(boostParameterName);
        }

        writer
                .append(")");
    }
}
