package net.ravendb.client.documents.session.tokens;

public class LoadToken extends QueryToken {

    public final String argument;
    public final String alias;

    private LoadToken(String argument, String alias) {
        this.argument = argument;
        this.alias = alias;
    }

    public static LoadToken create(String argument, String alias) {
        return new LoadToken(argument, alias);
    }

    @Override
    public void writeTo(StringBuilder writer) {
        writer
                .append(argument)
                .append(" as ")
                .append(alias);
    }
}
