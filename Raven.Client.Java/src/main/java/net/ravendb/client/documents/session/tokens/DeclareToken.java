package net.ravendb.client.documents.session.tokens;

public class DeclareToken extends QueryToken {

    private final String name;
    private final String parameters;
    private final String body;

    private DeclareToken(String name, String body, String parameters) {
        this.name = name;
        this.body = body;
        this.parameters = parameters;
    }

    public static DeclareToken create(String name, String body) {
        return create(name, body, null);
    }

    public static DeclareToken create(String name, String body, String parameters) {
        return new DeclareToken(name, body, parameters);
    }

    @Override
    public void writeTo(StringBuilder writer) {
        writer
                .append("declare ")
                .append("function ")
                .append(name)
                .append("(")
                .append(parameters)
                .append(") ")
                .append("{")
                .append(System.lineSeparator())
                .append(body)
                .append(System.lineSeparator())
                .append("}")
                .append(System.lineSeparator());
    }
}
