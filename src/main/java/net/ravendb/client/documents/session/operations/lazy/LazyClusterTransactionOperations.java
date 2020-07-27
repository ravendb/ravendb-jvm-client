package net.ravendb.client.documents.session.operations.lazy;

import net.ravendb.client.documents.Lazy;
import net.ravendb.client.documents.operations.compareExchange.CompareExchangeValue;
import net.ravendb.client.documents.session.ClusterTransactionOperationsBase;
import net.ravendb.client.documents.session.DocumentSession;
import net.ravendb.client.documents.session.ILazyClusterTransactionOperations;

import java.util.Map;
import java.util.function.Consumer;

@SuppressWarnings("unchecked")
public class LazyClusterTransactionOperations extends ClusterTransactionOperationsBase implements ILazyClusterTransactionOperations {

    public LazyClusterTransactionOperations(DocumentSession session) {
        super(session);
    }

    @Override
    public <T> Lazy<CompareExchangeValue<T>> getCompareExchangeValue(Class<T> clazz, String key) {
        return session.addLazyOperation((Class<CompareExchangeValue<T>>)(Class<?>)CompareExchangeValue.class,
                new LazyGetCompareExchangeValueOperation<>(this, clazz, session.getConventions(), key), null);
    }

    @Override
    public <T> Lazy<CompareExchangeValue<T>> getCompareExchangeValue(Class<T> clazz, String key, Consumer<CompareExchangeValue<T>> onEval) {
        return session.addLazyOperation((Class<CompareExchangeValue<T>>)(Class<?>)CompareExchangeValue.class,
                new LazyGetCompareExchangeValueOperation<>(this, clazz, session.getConventions(), key), onEval);
    }

    @Override
    public <T> Lazy<Map<String, CompareExchangeValue<T>>> getCompareExchangeValues(Class<T> clazz, String[] keys) {
        return session.addLazyOperation((Class<Map<String, CompareExchangeValue<T>>>)(Class<?>)Map.class,
                new LazyGetCompareExchangeValuesOperation<>(this, clazz, session.getConventions(), keys), null);
    }

    @Override
    public <T> Lazy<Map<String, CompareExchangeValue<T>>> getCompareExchangeValues(Class<T> clazz, String[] keys, Consumer<Map<String, CompareExchangeValue<T>>> onEval) {
        return session.addLazyOperation((Class<Map<String, CompareExchangeValue<T>>>)(Class<?>)Map.class,
                new LazyGetCompareExchangeValuesOperation<>(this, clazz, session.getConventions(), keys), onEval);
    }
}
