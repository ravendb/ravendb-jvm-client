package net.ravendb.client.documents.session.tokens;

import java.util.HashSet;
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
        RQL_KEYWORDS.add("AS");
        RQL_KEYWORDS.add("SELECT");
        RQL_KEYWORDS.add("WHERE");
        RQL_KEYWORDS.add("LOAD");
        RQL_KEYWORDS.add("GROUP");
        RQL_KEYWORDS.add("ORDER");
        RQL_KEYWORDS.add("INCLUDE");

    }
}
