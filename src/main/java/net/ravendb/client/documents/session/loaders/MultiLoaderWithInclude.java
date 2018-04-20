package net.ravendb.client.documents.session.loaders;

import net.ravendb.client.documents.session.IDocumentSessionImpl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Fluent implementation for specifying include paths
 * for loading documents
 */
public class MultiLoaderWithInclude implements ILoaderWithInclude {

    private final IDocumentSessionImpl _session;
    private final List<String> _includes = new ArrayList<>();

    /**
     * Includes the specified path.
     * @param path Path to include
     * @return loader with includes
     */
    @Override
    public ILoaderWithInclude include(String path) {
        _includes.add(path);
        return this;
    }

    /**
     * Loads the specified ids.
     * @param <TResult> Result class
     * @param clazz Result class
     * @param ids Ids to load
     * @return Map: id to entity
     */
    @Override
    public <TResult> Map<String, TResult> load(Class<TResult> clazz, String... ids) {
        return _session.loadInternal(clazz, ids, _includes.toArray(new String[0]));
    }

    /**
     * Loads the specified ids.
     * @param <TResult> Result class
     * @param clazz Result class
     * @param ids Ids to load
     * @return Map: id to entity
     */
    @Override
    public <TResult> Map<String, TResult> load(Class<TResult> clazz, Collection<String> ids) {
        return _session.loadInternal(clazz, ids.toArray(new String[0]), _includes.toArray(new String[0]));
    }

    /**
     * Loads the specified id.
     * @param <TResult> Result class
     * @param clazz Result class
     * @param id Id to load
     * @return Loaded entity
     */
    @Override
    public <TResult> TResult load(Class<TResult> clazz, String id) {
        Map<String, TResult> stringObjectMap = _session.loadInternal(clazz, new String[]{ id }, _includes.toArray(new String[0]));
        return stringObjectMap.values().stream().filter(x -> x != null).findFirst().orElse(null);
    }

    /**
     * Initializes a new instance of the MultiLoaderWithInclude class
     * @param session Session
     */
    public MultiLoaderWithInclude(IDocumentSessionImpl session) {
        _session = session;
    }

}
