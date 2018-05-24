package net.ravendb.client.documents.session.tokens;

public class SuggestToken extends QueryToken {

    private final String _fieldName;
    private final String _termParameterName;
    private final String _optionsParameterName;

    private SuggestToken(String fieldName, String termParameterName, String optionsParameterName) {
        if (fieldName == null) {
            throw new IllegalArgumentException("fieldName cannot be null");
        }

        if (termParameterName == null) {
            throw new IllegalArgumentException("termParameterName cannot be null");
        }

        _fieldName = fieldName;
        _termParameterName = termParameterName;
        _optionsParameterName = optionsParameterName;
    }

    public static SuggestToken create(String fieldName, String termParameterName, String optionsParameterName) {
        return new SuggestToken(fieldName, termParameterName, optionsParameterName);
    }

    @Override
    public void writeTo(StringBuilder writer) {
        writer
                .append("suggest(")
                .append(_fieldName)
                .append(", $")
                .append(_termParameterName);

        if (_optionsParameterName != null) {
            writer
                    .append(", $")
                    .append(_optionsParameterName);
        }

        writer
                .append(")");
    }

}
