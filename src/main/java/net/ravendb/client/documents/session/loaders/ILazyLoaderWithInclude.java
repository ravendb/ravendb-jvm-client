package net.ravendb.client.documents.session.loaders;

import net.ravendb.client.documents.Lazy;

import java.util.Collection;
import java.util.Map;

public interface ILazyLoaderWithInclude {

    //TBD expr overrides with expressions + maybe we TInclude, see:

    /**
     * Begin a load while including the specified path
     * @param path Path in documents in which server should look for a 'referenced' documents.
     * @return Lazy loader with includes support
     */
    ILazyLoaderWithInclude include(String path);

    /**
     * Loads the specified entities with the specified ids.
     * @param clazz Result class
     * @param ids  Ids that should be loaded
     * @param <T> Result class
     * @return Lazy Map: id to entity
     */
    <T> Lazy<Map<String, T>> load(Class<T> clazz, String... ids);

    /**
     * Loads the specified ids.
     * @param <TResult> Result class
     * @param clazz Result class
     * @param ids Ids to load
     * @return Lazy Map: id to entity
     */
    <TResult> Lazy<Map<String, TResult>> load(Class<TResult> clazz, Collection<String> ids);

    /**
     * Loads the specified entity with the specified id.
     * @param clazz Result class
     * @param id Identifier of document
     * @param <TResult> Result class
     * @return Lazy result
     */
    <TResult> Lazy<TResult> load(Class<TResult> clazz, String id);
}
