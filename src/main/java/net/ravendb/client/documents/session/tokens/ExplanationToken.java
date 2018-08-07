package net.ravendb.client.documents.session.tokens;

public class ExplanationToken extends QueryToken {
    private final String _optionsParameterName;

    private ExplanationToken(String optionsParameterName) {
        _optionsParameterName = optionsParameterName;
    }

    public static ExplanationToken create(String optionsParameterName) {
        return new ExplanationToken(optionsParameterName);
    }

    @Override
    public void writeTo(StringBuilder writer) {
        writer
                .append("explanations(");

        if (_optionsParameterName != null) {
            writer
                    .append("$")
                    .append(_optionsParameterName);
        }

        writer.append(")");
    }
}
