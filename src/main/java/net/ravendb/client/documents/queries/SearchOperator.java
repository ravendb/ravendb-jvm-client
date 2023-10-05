package net.ravendb.client.documents.queries;

/**
 * Search operator between terms in a search clause
 */
public enum SearchOperator {
    /**
     * Or operator will be used between all terms of a search clause.
     * A field value that matches any of the terms will be considered a match.
     */
    OR,

    /**
     * And operator will be used between all terms of a search clause.
     * A field value matching all of the terms will be considered a match.
     */
    AND
}
