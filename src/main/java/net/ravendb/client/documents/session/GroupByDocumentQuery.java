package net.ravendb.client.documents.session;

import net.ravendb.client.documents.queries.FilterFactory;
import net.ravendb.client.documents.queries.IFilterFactory;
import net.ravendb.client.primitives.CleanCloseable;

import java.util.function.Consumer;

public class GroupByDocumentQuery<T> implements IGroupByDocumentQuery<T> {

    private final DocumentQuery<T> _query;

    public GroupByDocumentQuery(DocumentQuery<T> query) {
        _query = query;
    }

    @Override
    public IGroupByDocumentQuery<T> selectKey() {
        return selectKey(null, null);
    }

    @Override
    public IGroupByDocumentQuery<T> selectKey(String fieldName) {
        return selectKey(fieldName, null);
    }

    @Override
    public IGroupByDocumentQuery<T> selectKey(String fieldName, String projectedName) {
        _query._groupByKey(fieldName, projectedName);
        return this;
    }

    @Override
    public IDocumentQuery<T> selectSum(GroupByField field, GroupByField... fields) {
        if (field == null) {
            throw new IllegalArgumentException("Field cannot be null");
        }

        _query._groupBySum(field.getFieldName(), field.getProjectedName());

        if (fields == null || fields.length == 0) {
            return _query;
        }

        for (GroupByField f : fields) {
            _query._groupBySum(f.getFieldName(), f.getProjectedName());
        }

        return _query;
    }

    @Override
    public IDocumentQuery<T> selectCount() {
        return selectCount("count");
    }

    @Override
    public IDocumentQuery<T> selectCount(String projectedName) {
        _query._groupByCount(projectedName);
        return _query;
    }


    @Override
    public IGroupByDocumentQuery<T> filter(Consumer<IFilterFactory<T>> builder) {
        return filter(builder, Integer.MAX_VALUE);
    }
    @Override
    public IGroupByDocumentQuery<T> filter(Consumer<IFilterFactory<T>> builder, int limit) {
        try (CleanCloseable mode = _query.setFilterMode(true)) {
            FilterFactory<T> f = new FilterFactory<>(_query, limit);
            builder.accept(f);
        }

        return this;
    }
}
