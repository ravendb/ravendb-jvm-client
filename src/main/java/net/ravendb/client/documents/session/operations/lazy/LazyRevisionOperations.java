package net.ravendb.client.documents.session.operations.lazy;

import net.ravendb.client.documents.Lazy;
import net.ravendb.client.documents.session.DocumentSession;
import net.ravendb.client.documents.session.ILazyRevisionsOperations;
import net.ravendb.client.documents.session.operations.GetRevisionOperation;
import net.ravendb.client.json.MetadataAsDictionary;

import java.util.Date;
import java.util.List;
import java.util.Map;

public class LazyRevisionOperations implements ILazyRevisionsOperations {

    protected final DocumentSession delegate;

    public LazyRevisionOperations(DocumentSession delegate) {
        this.delegate = delegate;
    }

    @Override
    public <T> Lazy<T> get(Class<T> clazz, String changeVector) {
        GetRevisionOperation operation = new GetRevisionOperation(delegate, changeVector);
        LazyRevisionOperation lazyRevisionOperation = new LazyRevisionOperation(clazz, operation, LazyRevisionOperation.Mode.SINGLE);
        return delegate.addLazyOperation(clazz, lazyRevisionOperation, null);
    }

    @Override
    public <T> Lazy<List<MetadataAsDictionary>> getMetadataFor(String id) {
        return getMetadataFor(id, 0, 25);
    }

    @Override
    public <T> Lazy<List<MetadataAsDictionary>> getMetadataFor(String id, int start) {
        return getMetadataFor(id, start, 25);
    }

    @Override
    public <T> Lazy<List<MetadataAsDictionary>> getMetadataFor(String id, int start, int pageSize) {
        GetRevisionOperation operation = new GetRevisionOperation(delegate, id, start, pageSize);
        LazyRevisionOperation<MetadataAsDictionary> lazyRevisionOperation = new LazyRevisionOperation<>(MetadataAsDictionary.class, operation, LazyRevisionOperation.Mode.LIST_OF_METADATA);
        return delegate.addLazyOperation((Class<List<MetadataAsDictionary>>)(Class<?>)List.class, lazyRevisionOperation, null);
    }

    @Override
    public <T> Lazy<Map<String, T>> get(Class<T> clazz, String[] changeVectors) {
        GetRevisionOperation operation = new GetRevisionOperation(delegate, changeVectors);
        LazyRevisionOperation<T> lazyRevisionOperation = new LazyRevisionOperation<>(clazz, operation, LazyRevisionOperation.Mode.MAP);
        return delegate.addLazyOperation((Class<Map<String, T>>)(Class<?>)Map.class, lazyRevisionOperation, null);
    }

    @Override
    public <T> Lazy<T> get(Class<T> clazz, String id, Date date) {
        GetRevisionOperation operation = new GetRevisionOperation(delegate, id, date);
        LazyRevisionOperation<T> lazyRevisionOperation = new LazyRevisionOperation<>(clazz, operation, LazyRevisionOperation.Mode.SINGLE);
        return delegate.addLazyOperation(clazz, lazyRevisionOperation, null);
    }

    @Override
    public <T> Lazy<List<T>> getFor(Class<T> clazz, String id) {
        return getFor(clazz, id, 0, 25);
    }

    @Override
    public <T> Lazy<List<T>> getFor(Class<T> clazz, String id, int start) {
        return getFor(clazz, id, start, 25);
    }

    @Override
    public <T> Lazy<List<T>> getFor(Class<T> clazz, String id, int start, int pageSize) {
        GetRevisionOperation operation = new GetRevisionOperation(delegate, id, start, pageSize);
        LazyRevisionOperation<T> lazyRevisionOperation = new LazyRevisionOperation<>(clazz, operation, LazyRevisionOperation.Mode.MULTI);
        return delegate.addLazyOperation((Class<List<T>>)(Class<?>)List.class, lazyRevisionOperation, null);
    }
}
