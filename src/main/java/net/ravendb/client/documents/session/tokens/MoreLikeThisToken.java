package net.ravendb.client.documents.session.tokens;

import net.ravendb.client.documents.session.DocumentQueryHelper;

import java.util.LinkedList;

public class MoreLikeThisToken extends WhereToken {

    public String documentParameterName;

    public String optionsParameterName;

    public final LinkedList<QueryToken> whereTokens;

    public MoreLikeThisToken() {
        whereTokens = new LinkedList<>();
    }

    @Override
    public void writeTo(StringBuilder writer) {
        writer.append("moreLikeThis(");

        if (documentParameterName == null) {
            for (int i = 0; i < whereTokens.size(); i++) {
                DocumentQueryHelper.addSpaceIfNeeded(i > 0 ? whereTokens.get(i - 1) : null, whereTokens.get(i), writer);
                whereTokens.get(i).writeTo(writer);
            }
        } else {
            writer.append("$")
                    .append(documentParameterName);
        }

        if (optionsParameterName == null) {
            writer.append(")");
            return;
        }

        writer.append(", $")
            .append(optionsParameterName)
            .append(")");
    }
}
