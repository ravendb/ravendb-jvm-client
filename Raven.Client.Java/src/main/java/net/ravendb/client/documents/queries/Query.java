package net.ravendb.client.documents.queries;

public class Query {

    private String collection;
    private String indexName;

    private Query() {
    }

    public String getCollection() {
        return collection;
    }

    public String getIndexName() {
        return indexName;
    }

    public static Query index(String indexName) {
        Query query = new Query();
        query.indexName = indexName;
        return query;
    }

    public static Query collection(String collectionName) {
        Query query = new Query();
        query.collection = collectionName;
        return query;
    }
}
