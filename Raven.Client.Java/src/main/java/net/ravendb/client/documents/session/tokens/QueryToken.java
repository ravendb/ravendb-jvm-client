package net.ravendb.client.documents.session.tokens;

import java.util.Set;
import java.util.TreeSet;

public abstract class QueryToken {

    public abstract void writeTo(StringBuilder writer);

    protected void writeField(StringBuilder writer, String field) {
        boolean keyWord = RQL_KEYWORDS.contains(field);
        if (keyWord) {
            writer.append("'");
        }
        writer.append(field);

        if (keyWord) {
            writer.append("'");
        }
    }

    private static final Set<String> RQL_KEYWORDS = new TreeSet<>();

    static {
        RQL_KEYWORDS.add("as");
        RQL_KEYWORDS.add("select");
        RQL_KEYWORDS.add("where");
        RQL_KEYWORDS.add("load");
        RQL_KEYWORDS.add("group");
        RQL_KEYWORDS.add("order");
        RQL_KEYWORDS.add("include");

    }
}
