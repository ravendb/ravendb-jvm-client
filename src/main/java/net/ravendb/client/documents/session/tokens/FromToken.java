package net.ravendb.client.documents.session.tokens;

import net.ravendb.client.extensions.StringExtensions;

import java.util.Arrays;
import java.util.List;

public class FromToken extends QueryToken {

    private final String collectionName;
    private final String indexName;
    private final boolean dynamic;
    private final String alias;

    public String getCollectionName() {
        return collectionName;
    }

    public String getIndexName() {
        return indexName;
    }

    public boolean isDynamic() {
        return dynamic;
    }

    public String getAlias() {
        return alias;
    }

    private FromToken(String indexName, String collectionName) {
        this(indexName, collectionName, null);
    }

    private FromToken(String indexName, String collectionName, String alias) {
        this.collectionName = collectionName;
        this.indexName = indexName;
        this.dynamic = collectionName != null;
        this.alias = alias;
    }

    public static FromToken create(String indexName, String collectionName, String alias) {
        return new FromToken(indexName, collectionName, alias);
    }

    private static final List<Character> WHITE_SPACE_CHARS = Arrays.asList(' ', '\t', '\r', '\n');

    @Override
    public void writeTo(StringBuilder writer) {
        if (indexName == null && collectionName == null) {
            throw new IllegalStateException("Either indexName or collectionName must be specified");
        }

        if (dynamic) {
            writer.append("from '");
            StringExtensions.escapeString(writer, collectionName);
            writer.append("'");
        } else {
            writer
                    .append("from index '")
                    .append(indexName)
                    .append("'");
        }

        if (alias != null) {
            writer.append(" as ").append(alias);
        }
    }
}
