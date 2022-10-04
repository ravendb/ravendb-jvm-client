package net.ravendb.client.documents.queries;

import net.ravendb.client.documents.session.DocumentQuery;
import net.ravendb.client.documents.session.IAbstractDocumentQuery;
import net.ravendb.client.documents.session.MethodCall;
import net.ravendb.client.documents.session.WhereParams;

public class FilterFactory<T> implements IFilterFactory<T> {

    private IAbstractDocumentQuery<T> _documentQuery;

    public FilterFactory(IAbstractDocumentQuery<T> documentQuery) {
        this(documentQuery, Integer.MAX_VALUE);
    }

    public FilterFactory(IAbstractDocumentQuery<T> documentQuery, int filterLimit) {
        _documentQuery = documentQuery;
        setFilterLimit(filterLimit);
    }

    @Override
    public IFilterFactory<T> equals(String fieldName, MethodCall value) {
        _documentQuery._whereEquals(fieldName, value);
        return this;
    }

    @Override
    public IFilterFactory<T> equals(String fieldName, Object value) {
        _documentQuery._whereEquals(fieldName, value);
        return this;
    }

    @Override
    public IFilterFactory<T> equals(WhereParams whereParams) {
        _documentQuery._whereEquals(whereParams);
        return this;
    }

    @Override
    public IFilterFactory<T> notEquals(String fieldName, Object value) {
        _documentQuery._whereNotEquals(fieldName, value);
        return this;
    }

    @Override
    public IFilterFactory<T> notEquals(String fieldName, MethodCall method) {
        _documentQuery._whereNotEquals(fieldName, method);
        return this;
    }

    @Override
    public IFilterFactory<T> notEquals(WhereParams whereParams) {
        _documentQuery._whereNotEquals(whereParams);
        return this;
    }

    @Override
    public IFilterFactory<T> greaterThan(String fieldName, Object value) {
        _documentQuery._whereGreaterThan(fieldName, value);
        return this;
    }

    @Override
    public IFilterFactory<T> greaterThanOrEqual(String fieldName, Object value) {
        _documentQuery._whereGreaterThanOrEqual(fieldName, value);
        return this;
    }

    @Override
    public IFilterFactory<T> lessThan(String fieldName, Object value) {
        _documentQuery._whereLessThan(fieldName, value);
        return this;
    }

    @Override
    public IFilterFactory<T> lessThanOrEqual(String fieldName, Object value) {
        _documentQuery._whereLessThanOrEqual(fieldName, value);
        return this;
    }

    @Override
    public IFilterFactory<T> andAlso() {
        _documentQuery._andAlso();
        return this;
    }

    @Override
    public IFilterFactory<T> orElse() {
        _documentQuery._orElse();
        return this;
    }

    @Override
    public IFilterFactory<T> not() {
        _documentQuery._negateNext();
        return this;
    }

    @Override
    public IFilterFactory<T> openSubclause() {
        _documentQuery._openSubclause();
        return this;
    }

    @Override
    public IFilterFactory<T> closeSubclause() {
        _documentQuery._closeSubclause();
        return this;
    }

    private void setFilterLimit(int limit) {
        ((DocumentQuery<T>)_documentQuery)._addFilterLimit(limit);
    }
}
