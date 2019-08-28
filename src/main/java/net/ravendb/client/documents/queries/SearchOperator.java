package net.ravendb.client.documents.queries;

/**
 * Search operator between terms in a search caluse
 */
public enum SearchOperator {
    /**
     * Or operator will be used between all terms for a search clause, meaning a field value that matches any of the terms will be considered a match
     */
    OR,

    /**
     * And operator will be used between all terms for a search clause, meaning a field value matching all of the terms will be considered a match
     */
    AND
}
