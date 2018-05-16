package net.ravendb.client.documents.session.loaders;

import java.util.Collection;
import java.util.Map;

/**
 * Fluent interface for specifying include paths
 * for loading documents
 */
public interface ILoaderWithInclude {

    //TBD expr overrides with expressions + maybe we TInclude, see:

    /**
     * Includes the specified path.
     * @param path Path to include
     * @return Loader with includes
     */
    ILoaderWithInclude include(String path);

    /**
     * Loads the specified ids.
     * @param <TResult> Result class
     * @param clazz Result class
     * @param ids Ids to load
     * @return Map: id to entity
     */
    <TResult> Map<String, TResult> load(Class<TResult> clazz, String... ids);

    /**
     * Loads the specified ids.
     * @param <TResult> Result class
     * @param clazz Result class
     * @param ids Ids to load
     * @return Map: id to entity
     */
    <TResult> Map<String, TResult> load(Class<TResult> clazz, Collection<String> ids);

    /**
     * Loads the specified id.
     * @param <TResult> Result class
     * @param clazz Result class
     * @param id Id to load
     * @return Loaded entity
     */
    <TResult> TResult load(Class<TResult> clazz, String id);

}
