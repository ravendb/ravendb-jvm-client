package net.ravendb.client.documents.session.loaders;

import java.util.Collection;
import java.util.Map;

/**
 * Fluent interface for specifying include paths
 * for loading documents
 */
public interface ILoaderWithInclude {

    //TBD: overrides with expressions + maybe we TInclude, see:

    /**
     * Includes the specified path.
     */
    ILoaderWithInclude include(String path);

    /**
     * Loads the specified ids.
     */
    <TResult> Map<String, TResult> load(Class<TResult> clazz, String... ids);

    /**
     * Loads the specified ids.
     */
    <TResult> Map<String, TResult> load(Class<TResult> clazz, Collection<String> ids);

    /**
     * Loads the specified id.
     */
    <TResult> TResult load(Class<TResult> clazz, String id);

}
