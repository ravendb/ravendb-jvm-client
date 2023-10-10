package net.ravendb.client.documents.session.tokens;

import net.ravendb.client.documents.queries.QueryFieldUtil;
import org.apache.commons.lang3.StringUtils;

public class SuggestToken extends QueryToken {

    private final String _fieldName;
    private final String _alias;
    private final String _termParameterName;
    private final String _optionsParameterName;

    private SuggestToken(String fieldName, String alias, String termParameterName, String optionsParameterName) {
        if (fieldName == null) {
            throw new IllegalArgumentException("fieldName cannot be null");
        }

        if (termParameterName == null) {
            throw new IllegalArgumentException("termParameterName cannot be null");
        }

        _fieldName = fieldName;
        _alias = alias;
        _termParameterName = termParameterName;
        _optionsParameterName = optionsParameterName;
    }

    public static SuggestToken create(String fieldName, String alias, String termParameterName, String optionsParameterName) {
        return new SuggestToken(fieldName, QueryFieldUtil.escapeIfNecessary(alias), termParameterName, optionsParameterName);
    }

    public String getFieldName() {
        return _fieldName;
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

        if (StringUtils.isBlank(_alias) || getFieldName().equals(_alias)) {
            return;
        }

        writer
                .append(" as ")
                .append(_alias);
    }
}
