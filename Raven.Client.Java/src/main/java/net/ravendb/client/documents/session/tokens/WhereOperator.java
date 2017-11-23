package net.ravendb.client.documents.session.tokens;

public enum WhereOperator {
    EQUALS,
    NOT_EQUALS,
    GREATER_THAN,
    GREATER_THAN_OR_EQUAL,
    LESS_THAN,
    LESS_THAN_OR_EQUAL,
    IN,
    ALL_IN,
    BETWEEN,
    SEARCH,
    LUCENE,
    STARTS_WITH,
    ENDS_WITH,
    EXISTS,
    WITHIN,
    CONTAINS,
    DISJOINT,
    INTERSECTS,
    REGEX,
    CMP_X_CHG_MATCH
}
