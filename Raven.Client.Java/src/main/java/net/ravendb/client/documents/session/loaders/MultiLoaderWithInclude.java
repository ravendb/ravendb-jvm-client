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
     */
    @Override
    public ILoaderWithInclude include(String path) {
        _includes.add(path);
        return this;
    }

    /**
     * Loads the specified ids.
     */
    @Override
    public <TResult> Map<String, TResult> load(Class<TResult> clazz, String... ids) {
        return _session.loadInternal(clazz, ids, _includes.toArray(new String[0]));
    }

    /**
     * Loads the specified ids.
     */
    @Override
    public <TResult> Map<String, TResult> load(Class<TResult> clazz, Collection<String> ids) {
        return _session.loadInternal(clazz, ids.toArray(new String[0]), _includes.toArray(new String[0]));
    }

    /**
     * Loads the specified id.
     */
    @Override
    public <TResult> TResult load(Class<TResult> clazz, String id) {
        Map<String, TResult> stringObjectMap = _session.loadInternal(clazz, new String[]{ id }, _includes.toArray(new String[0]));
        return stringObjectMap.values().stream().findFirst().orElse(null);
    }

    /**
     * Initializes a new instance of the MultiLoaderWithInclude class
     */
    public MultiLoaderWithInclude(IDocumentSessionImpl session) {
        _session = session;
    }

}
