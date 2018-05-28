package net.ravendb.client.documents.session.loaders;

import net.ravendb.client.documents.Lazy;
import net.ravendb.client.documents.session.IDocumentSessionImpl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class LazyMultiLoaderWithInclude implements ILazyLoaderWithInclude {

    private final IDocumentSessionImpl _session;
    private final List<String> _includes = new ArrayList<>();

    public LazyMultiLoaderWithInclude(IDocumentSessionImpl session) {
        _session = session;
    }

    /**
     * Includes the specified path.
     */
    @Override
    public ILazyLoaderWithInclude include(String path) {
        _includes.add(path);
        return this;
    }

    /**
     * Loads the specified ids.
     */
    @Override
    public <T> Lazy<Map<String, T>> load(Class<T> clazz, String... ids) {
        return _session.lazyLoadInternal(clazz, ids, _includes.toArray(new String[0]), null);
    }

    /**
     * Loads the specified ids.
     */
    @Override
    public <TResult> Lazy<Map<String, TResult>> load(Class<TResult> clazz, Collection<String> ids) {
        return _session.lazyLoadInternal(clazz, ids.toArray(new String[0]), _includes.toArray(new String[0]), null);
    }

    /**
     * Loads the specified id.
     */
    @SuppressWarnings("unchecked")
    @Override
    public <TResult> Lazy<TResult> load(Class<TResult> clazz, String id) {
        Lazy<Map<String, TResult>> results = _session.lazyLoadInternal(clazz, new String[]{id}, _includes.toArray(new String[0]), null);
        return new Lazy(() -> results.getValue().values().iterator().next());
    }
}
