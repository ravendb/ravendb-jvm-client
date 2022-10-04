package net.ravendb.client.documents.queries;

import net.ravendb.client.documents.session.MethodCall;
import net.ravendb.client.documents.session.WhereParams;

public interface IFilterFactory<T> {

    /**
     * Matches value
     * @param fieldName field name
     * @param method method
     * @return factory
     */
    IFilterFactory<T> equals(String fieldName, MethodCall method);

    /**
     * Matches value
     * @param fieldName field name
     * @param value value
     * @return factory
     */
    IFilterFactory<T> equals(String fieldName, Object value);

    /**
     * Matches value
     * @param whereParams where params
     * @return factory
     */
    IFilterFactory<T> equals(WhereParams whereParams);

    /**
     * Not matches value
     * @param fieldName field
     * @param method method
     * @return factory
     */
    IFilterFactory<T> notEquals(String fieldName, MethodCall method);

    /**
     * Not matches value
     * @param fieldName field name
     * @param value value
     * @return factory
     */
    IFilterFactory<T> notEquals(String fieldName, Object value);

    /**
     * Not matches value
     * @param whereParams where params
     * @return factory
     */
    IFilterFactory<T> notEquals(WhereParams whereParams);

    /**
     * Matches fields where the value is greater than the specified value
     * @param fieldName field name
     * @param value value
     * @return factory
     */
    IFilterFactory<T> greaterThan(String fieldName, Object value);

    /**
     * Matches fields Where the value is greater than or equal to the specified value
     * @param fieldName field name
     * @param value value
     * @return factory
     */
    IFilterFactory<T> greaterThanOrEqual(String fieldName, Object value);

    /**
     * Matches fields where the value is less than the specified value
     * @param fieldName field name
     * @param value value
     * @return factory
     */
    IFilterFactory<T> lessThan(String fieldName, Object value);

    /**
     * Matches fields where the value is less than or equal to the specified value
     * @param fieldName field name
     * @param value value
     * @return factory
     */
    IFilterFactory<T> lessThanOrEqual(String fieldName, Object value);

    /**
     * Simplified method for opening a new clause within the query
     * @return factory
     */
    IFilterFactory<T> andAlso();

    /**
     * Add an OR to the query
     * @return factory
     */
    IFilterFactory<T> orElse();

    /**
     * Negate the next operation
     * @return factory
     */
    IFilterFactory<T> not();

    /**
     * Simplified method for opening a new clause within the query
     * @return factory
     */
    IFilterFactory<T> openSubclause();

    /**
     * Simplified method for closing a clause within the query
     * @return factory
     */
    IFilterFactory<T> closeSubclause();
}
