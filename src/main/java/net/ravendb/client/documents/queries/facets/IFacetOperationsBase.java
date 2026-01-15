package net.ravendb.client.documents.queries.facets;

public interface IFacetOperationsBase<T, TSelf> {
    /**
     * Sets a display name for this field in the results (optional).
     *
     * @param displayName The display name.
     */
    TSelf withDisplayName(String displayName);
    TSelf sumOn(String path);
    /**
     * Gets the sum of values for each group of documents per range specified.
     *
     * @param path        Path of the field to sum from the document.
     * @param displayName Sets a display name for this field in the results (optional).
     */
    TSelf sumOn(String path, String displayName);
    TSelf minOn(String path);
    /**
     * Gets the minimum value for each group of documents per range specified.
     *
     * @param path        Path of the field from the document.
     * @param displayName Sets a display name for this field in the results (optional).
     */
    TSelf minOn(String path, String displayName);
    TSelf maxOn(String path);
    /**
     * Gets the maximum value for each group of documents per range specified.
     *
     * @param path        Path of the field from the document.
     * @param displayName Sets a display name for this field in the results (optional).
     */

    TSelf maxOn(String path, String displayName);
    TSelf averageOn(String path);
    /**
     * Gets the average from values for each group of documents per range specified.
     *
     * @param path        Path of the field from the document.
     * @param displayName Sets a display name for this field in the results (optional).
     */
    TSelf averageOn(String path, String displayName);
}
