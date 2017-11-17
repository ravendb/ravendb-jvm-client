package net.ravendb.client.documents.session.tokens;

import java.util.stream.StreamSupport;

public class FromToken extends QueryToken {

    private String collectionName;
    private String indexName;
    private boolean dynamic;
    private String alias;

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

    private static final char[] WHITE_SPACE_CHARS = new char[] { ' ', '\t', '\r', '\n' }; //TODO: \v ?

    @Override
    public void writeTo(StringBuilder writer) {
        if (indexName == null && collectionName == null) {
            throw new IllegalStateException("Either indexName or collectionName must be specified");
        }

        if (dynamic) {
            writer.append("FROM ");

            /* TODO:
            if(CollectionName.IndexOfAny(_whiteSpaceChars) != -1)
                {
                    if (CollectionName.IndexOf('"') != -1)
                    {
                        ThrowInvalidcollectionName();
                    }
                    writer.Append('"').Append(CollectionName).Append('"');
                }
             */


            writeField(writer, collectionName); //TODO wrap this in else

            if (alias != null) {
                writer.append(" as ").append(alias);
            }
            return;
        }

        writer
                .append("FROM INDEX '")
                .append(indexName)
                .append("'");
    }

    private void throwInvalidCollectionName() {
        throw new IllegalArgumentException("Collection name cannot contain a quote, but was: " + collectionName);
    }
}
