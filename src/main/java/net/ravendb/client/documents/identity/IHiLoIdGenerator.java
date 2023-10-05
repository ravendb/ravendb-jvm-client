package net.ravendb.client.documents.identity;

public interface IHiLoIdGenerator {

    long generateNextIdFor(String database, String collectionName);

    long generateNextIdFor(String database, Class<?> type);

    long generateNextIdFor(String database, Object entity);

    String generateDocumentId(String database, Object entity);

}
