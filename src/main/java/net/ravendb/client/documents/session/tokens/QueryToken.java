package net.ravendb.client.documents.session.tokens;

import java.util.Set;
import java.util.TreeSet;

public abstract class QueryToken {

    public abstract void writeTo(StringBuilder writer);

    public static void writeField(StringBuilder writer, String field) {
        boolean keyWord = isKeyword(field);
        if (keyWord) {
            writer.append("'");
        }
        writer.append(field);

        if (keyWord) {
            writer.append("'");
        }
    }

    public static boolean isKeyword(String field) {
        return RQL_KEYWORDS.contains(field);
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
