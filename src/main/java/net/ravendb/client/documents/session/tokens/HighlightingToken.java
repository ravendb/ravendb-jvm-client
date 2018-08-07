package net.ravendb.client.documents.session.tokens;

public class HighlightingToken extends QueryToken {
    private final String _fieldName;
    private final int _fragmentLength;
    private final int _fragmentCount;
    private final String _optionsParameterName;

    private HighlightingToken(String fieldName, int fragmentLength, int fragmentCount, String operationsParameterName) {
        _fieldName = fieldName;
        _fragmentLength = fragmentLength;
        _fragmentCount = fragmentCount;
        _optionsParameterName = operationsParameterName;
    }

    public static HighlightingToken create(String fieldName, int fragmentLength, int fragmentCount, String optionsParameterName) {
        return new HighlightingToken(fieldName, fragmentLength, fragmentCount, optionsParameterName);
    }

    @Override
    public void writeTo(StringBuilder writer) {
        writer
                .append("highlight(");

        writeField(writer, _fieldName);

        writer
                .append(",")
                .append(_fragmentLength)
                .append(",")
                .append(_fragmentCount);

        if (_optionsParameterName != null) {
            writer
                    .append(",$")
                    .append(_optionsParameterName);
        }

        writer.append(")");
    }
}