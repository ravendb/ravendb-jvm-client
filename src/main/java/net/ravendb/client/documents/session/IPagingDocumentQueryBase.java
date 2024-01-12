package net.ravendb.client.documents.session;

public interface IPagingDocumentQueryBase<T, TSelf extends IPagingDocumentQueryBase<T, TSelf>> {

    /**
     * Skips the specified count.
     * @param count Items to skip
     * @return Query instance
     */
    TSelf skip(long count);

    /**
     * Takes the specified count.
     * @param count Amount of items to take
     * @return Query instance
     */
    TSelf take(long count);
}
