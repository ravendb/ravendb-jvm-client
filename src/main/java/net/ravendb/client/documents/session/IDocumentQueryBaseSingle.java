package net.ravendb.client.documents.session;

import net.ravendb.client.documents.Lazy;

import java.util.List;
import java.util.function.Consumer;

public interface IDocumentQueryBaseSingle<T> {

    /**
     * Returns first element or throws if sequence is empty.
     * @return first result
     */
    T first();

    /**
     * Returns first element or default value for type if sequence is empty.
     * @return first result of default
     */
    T firstOrDefault();

    /**
     * Returns first element or throws if sequence is empty or contains more than one element.
     * @return single result or throws
     */
    T single();

    /**
     * Returns first element or default value for given type if sequence is empty. Throws if sequence contains more than
     * one element.
     * @return single result, default or throws
     */
    T singleOrDefault();


    /**
     * Checks if the given query matches any records
     * @return true if the given query matches any records
     */
    boolean any();

    /**
     * Gets the total count of records for this query
     * @return total count of records
     */
    int count();


}
