package net.ravendb.client.documents.session.operations.lazy;

import net.ravendb.client.documents.Lazy;
import net.ravendb.client.documents.session.DocumentSession;
import net.ravendb.client.documents.session.loaders.ILazyLoaderWithInclude;
import net.ravendb.client.documents.session.loaders.LazyMultiLoaderWithInclude;
import net.ravendb.client.documents.session.operations.LoadOperation;
import net.ravendb.client.primitives.Tuple;
import org.apache.commons.lang3.StringUtils;

import java.util.Collection;
import java.util.Map;
import java.util.function.Consumer;

public class LazySessionOperations implements ILazySessionOperations {

    protected final DocumentSession delegate;

    public LazySessionOperations(DocumentSession delegate) {
        this.delegate = delegate;
    }

    @Override
    public ILazyLoaderWithInclude include(String path) {
        return new LazyMultiLoaderWithInclude(delegate).include(path);
    }

    @Override
    public <TResult> Lazy<TResult> load(Class<TResult> clazz, String id) {
        return load(clazz, id, null);
    }

    @Override
    public <TResult> Lazy<TResult> load(Class<TResult> clazz, String id, Consumer<TResult> onEval) {
        if (delegate.isLoaded(id)) {
            return new Lazy<>(() -> delegate.load(clazz, id));
        }

        LazyLoadOperation<TResult> lazyLoadOperation = new LazyLoadOperation<>(clazz, delegate, new LoadOperation(delegate).byId(id)).byId(id);
        return delegate.addLazyOperation(clazz, lazyLoadOperation, onEval);
    }

    @Override
    public <TResult> Lazy<Map<String, TResult>> loadStartingWith(Class<TResult> clazz, String idPrefix) {
        return loadStartingWith(clazz, idPrefix, null, 0, 25, null, null);
    }

    @Override
    public <TResult> Lazy<Map<String, TResult>> loadStartingWith(Class<TResult> clazz, String idPrefix, String matches) {
        return loadStartingWith(clazz, idPrefix, matches, 0, 25, null, null);
    }

    @Override
    public <TResult> Lazy<Map<String, TResult>> loadStartingWith(Class<TResult> clazz, String idPrefix, String matches, int start) {
        return loadStartingWith(clazz, idPrefix, matches, start, 25, null, null);
    }

    @Override
    public <TResult> Lazy<Map<String, TResult>> loadStartingWith(Class<TResult> clazz, String idPrefix, String matches, int start, int pageSize) {
        return loadStartingWith(clazz, idPrefix, matches, start, pageSize, null, null);
    }

    @Override
    public <TResult> Lazy<Map<String, TResult>> loadStartingWith(Class<TResult> clazz, String idPrefix, String matches, int start, int pageSize, String exclude) {
        return loadStartingWith(clazz, idPrefix, matches, start, pageSize, exclude, null);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <TResult> Lazy<Map<String, TResult>> loadStartingWith(Class<TResult> clazz, String idPrefix, String matches, int start, int pageSize, String exclude, String startAfter) {
        LazyStartsWithOperation<TResult> operation = new LazyStartsWithOperation<>(clazz, idPrefix, matches, exclude, start, pageSize, delegate, startAfter);

        return delegate.addLazyOperation((Class<Map<String, TResult>>)(Class<?>)Map.class, operation, null);
    }

    @Override
    public <TResult> Lazy<Map<String, TResult>> load(Class<TResult> clazz, Collection<String> ids) {
        return load(clazz, ids, null);
    }

    @Override
    public <TResult> Lazy<Map<String, TResult>> load(Class<TResult> clazz, Collection<String> ids, Consumer<Map<String, TResult>> onEval) {
        return delegate.lazyLoadInternal(clazz, ids.toArray(new String[0]), new String[0], onEval);
    }

    @Override
    public <TResult> Lazy<Tuple<TResult, String>> conditionalLoad(Class<TResult> clazz, String id, String changeVector) {
        if (StringUtils.isEmpty(id)) {
            throw new IllegalArgumentException("Id cannot be null");
        }

        if (delegate.isLoaded(id)) {
            return new Lazy<>(() -> {
                TResult entity = delegate.load(clazz, id);
                if (entity == null) {
                    return Tuple.create(null, null);
                }
                String cv = delegate.advanced().getChangeVectorFor(entity);
                return Tuple.create(entity, cv);
            });
        }

        if (StringUtils.isEmpty(changeVector)) {
            throw new IllegalArgumentException("The requested document with id '"
                    + id + "' is not loaded into the session and could not conditional load when changeVector is null or empty.");
        }

        LazyConditionalLoadOperation<TResult> lazyLoadOperation = new LazyConditionalLoadOperation<>(clazz, id, changeVector, delegate);
        return delegate.addLazyOperation((Class<Tuple<TResult, String>>)clazz, lazyLoadOperation, null);
    }

    //TBD expr ILazyLoaderWithInclude<T> ILazySessionOperations.Include<T>(Expression<Func<T, string>> path)
    //TBD expr ILazyLoaderWithInclude<T> ILazySessionOperations.Include<T>(Expression<Func<T, IEnumerable<string>>> path)
}
