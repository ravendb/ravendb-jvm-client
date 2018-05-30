package net.ravendb.client.documents.session.operations.lazy;

import net.ravendb.client.documents.Lazy;
import net.ravendb.client.documents.session.loaders.ILazyLoaderWithInclude;

import java.util.Collection;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Specify interface for lazy operation for the session
 */
public interface ILazySessionOperations {

    /**
     * Begin a load while including the specified path
     * @param path Path in documents in which server should look for a 'referenced' documents.
     * @return Lazy loader with includes support
     */
    ILazyLoaderWithInclude include(String path);

    //TBD expr ILazyLoaderWithInclude<TResult> Include<TResult>(Expression<Func<TResult, string>> path);

    //TBD expr ILazyLoaderWithInclude<TResult> Include<TResult>(Expression<Func<TResult, IEnumerable<string>>> path);

    /**
     * Loads the specified entities with the specified ids.
     * @param clazz Result class
     * @param ids Ids of documents that should be lazy loaded
     * @param <TResult> Result class
     * @return Lazy map of results
     */
    <TResult> Lazy<Map<String, TResult>> load(Class<TResult> clazz, Collection<String> ids);

    /**
     * Loads the specified entities with the specified ids.
     * @param clazz Result class
     * @param ids Ids of documents that should be lazy loaded
     * @param onEval Action to be executed on evaluation.
     * @param <TResult> Result class
     * @return Lazy map of results
     */
    <TResult> Lazy<Map<String, TResult>> load(Class<TResult> clazz, Collection<String> ids, Consumer<Map<String, TResult>> onEval);


    /**
     * Loads the specified entity with the specified id.
     * @param clazz Result class
     * @param id Identifier of a entity that will be loaded.
     * @param <TResult> Result class
     * @return Lazy loaded document
     */
    <TResult> Lazy<TResult> load(Class<TResult> clazz, String id);

    /**
     * Loads the specified entity with the specified id.
     * @param clazz Result class
     * @param id Identifier of a entity that will be loaded.
     * @param onEval Action to be executed on evaluation.
     * @param <TResult> Result class
     * @return Lazy loaded document
     */
    <TResult> Lazy<TResult> load(Class<TResult> clazz, String id, Consumer<TResult> onEval);

    /**
     * Loads multiple entities that contain common prefix.
     * @param clazz Result class
     * @param idPrefix prefix for which documents should be returned e.g. "products/"
     * @param <TResult> Result class
     * @return Lazy map of results
     */
    <TResult> Lazy<Map<String, TResult>> loadStartingWith(Class<TResult> clazz, String idPrefix);

    /**
     * Loads multiple entities that contain common prefix.
     * @param clazz Result class
     * @param idPrefix prefix for which documents should be returned e.g. "products/"
     * @param matches pipe ('|') separated values for which document IDs (after 'idPrefix') should be matched ('?' any single character, '*' any characters)
     * @param <TResult> Result class
     * @return Lazy map of results
     */
    <TResult> Lazy<Map<String, TResult>> loadStartingWith(Class<TResult> clazz, String idPrefix, String matches);

    /**
     * Loads multiple entities that contain common prefix.
     * @param clazz Result class
     * @param idPrefix prefix for which documents should be returned e.g. "products/"
     * @param matches pipe ('|') separated values for which document IDs (after 'idPrefix') should be matched ('?' any single character, '*' any characters)
     * @param start number of documents that should be skipped. By default: 0.
     * @param <TResult> Result class
     * @return Lazy map of results
     */
    <TResult> Lazy<Map<String, TResult>> loadStartingWith(Class<TResult> clazz, String idPrefix, String matches, int start);

    /**
     * Loads multiple entities that contain common prefix.
     * @param clazz Result class
     * @param idPrefix prefix for which documents should be returned e.g. "products/"
     * @param matches pipe ('|') separated values for which document IDs (after 'idPrefix') should be matched ('?' any single character, '*' any characters)
     * @param start number of documents that should be skipped. By default: 0.
     * @param pageSize maximum number of documents that will be retrieved. By default: 25.
     * @param <TResult> Result class
     * @return Lazy map of results
     */
    <TResult> Lazy<Map<String, TResult>> loadStartingWith(Class<TResult> clazz, String idPrefix, String matches, int start, int pageSize);

    /**
     * Loads multiple entities that contain common prefix.
     * @param clazz Result class
     * @param idPrefix prefix for which documents should be returned e.g. "products/"
     * @param matches pipe ('|') separated values for which document IDs (after 'idPrefix') should be matched ('?' any single character, '*' any characters)
     * @param start number of documents that should be skipped. By default: 0.
     * @param pageSize maximum number of documents that will be retrieved. By default: 25.
     * @param exclude pipe ('|') separated values for which document IDs (after 'idPrefix') should not be matched ('?' any single character, '*' any characters)
     * @param <TResult> Result class
     * @return Lazy map of results
     */
    <TResult> Lazy<Map<String, TResult>> loadStartingWith(Class<TResult> clazz, String idPrefix, String matches, int start, int pageSize, String exclude);

    /**
     * Loads multiple entities that contain common prefix.
     * @param clazz Result class
     * @param idPrefix prefix for which documents should be returned e.g. "products/"
     * @param matches pipe ('|') separated values for which document IDs (after 'idPrefix') should be matched ('?' any single character, '*' any characters)
     * @param start number of documents that should be skipped. By default: 0.
     * @param pageSize maximum number of documents that will be retrieved. By default: 25.
     * @param exclude pipe ('|') separated values for which document IDs (after 'idPrefix') should not be matched ('?' any single character, '*' any characters)
     * @param startAfter skip document fetching until given ID is found and return documents after that ID (default: null)
     * @param <TResult> Result class
     * @return Lazy map of results
     */
    <TResult> Lazy<Map<String, TResult>> loadStartingWith(Class<TResult> clazz, String idPrefix, String matches, int start, int pageSize, String exclude, String startAfter);
}
