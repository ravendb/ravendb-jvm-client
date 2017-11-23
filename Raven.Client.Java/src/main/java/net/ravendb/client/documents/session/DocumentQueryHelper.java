package net.ravendb.client.documents.session;

import net.ravendb.client.documents.session.tokens.CloseSubclauseToken;
import net.ravendb.client.documents.session.tokens.IntersectMarkerToken;
import net.ravendb.client.documents.session.tokens.OpenSubclauseToken;
import net.ravendb.client.documents.session.tokens.QueryToken;

public class DocumentQueryHelper {
    public static void addSpaceIfNeeded(QueryToken previousToken, QueryToken currentToken, StringBuilder writer) {
        if (previousToken == null) {
            return;
        }

        if (previousToken instanceof OpenSubclauseToken || currentToken instanceof CloseSubclauseToken || currentToken instanceof IntersectMarkerToken) {
            return;
        }
        writer.append(" ");
    }
}
