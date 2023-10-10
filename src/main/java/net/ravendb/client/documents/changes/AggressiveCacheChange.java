package net.ravendb.client.documents.changes;

public class AggressiveCacheChange extends DatabaseChange {

    public static final AggressiveCacheChange INSTANCE = new AggressiveCacheChange();

    public static boolean shouldUpdateAggressiveCache(DocumentChange change) {
        return DocumentChangeTypes.PUT.equals(change.getType()) || DocumentChangeTypes.DELETE.equals(change.getType());
    }
    public static boolean shouldUpdateAggressiveCache(IndexChange change) {
        return IndexChangeTypes.BATCH_COMPLETED.equals(change.getType()) || IndexChangeTypes.INDEX_REMOVED.equals(change.getType());
    }

}
