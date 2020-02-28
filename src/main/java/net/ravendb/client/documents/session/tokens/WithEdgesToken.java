package net.ravendb.client.documents.session.tokens;

import org.apache.commons.lang3.StringUtils;

public class WithEdgesToken extends QueryToken {

    private final String _alias;
    private final String _edgeSelector;
    private final String _query;

    public WithEdgesToken(String alias, String edgeSelector, String query) {
        _alias = alias;
        _query = query;
        _edgeSelector = edgeSelector;
    }

    @Override
    public void writeTo(StringBuilder writer) {
        writer.append("with edges(");
        writer.append(_edgeSelector);
        writer.append(")");

        if (!StringUtils.isBlank(_query)) {
            writer.append(" {");
            writer.append(_query);
            writer.append("} ");
        }

        writer.append(" as ");
        writer.append(_alias);
    }
}
