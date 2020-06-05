package net.ravendb.client.documents.timeSeries;

import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.operations.MaintenanceOperationExecutor;
import net.ravendb.client.documents.operations.timeSeries.*;
import net.ravendb.client.documents.session.timeSeries.TimeSeriesValue;
import net.ravendb.client.documents.session.timeSeries.TimeSeriesValuesHelper;
import net.ravendb.client.primitives.TimeValue;
import net.ravendb.client.primitives.Tuple;
import org.apache.commons.collections4.map.LinkedMap;

import java.lang.reflect.Field;
import java.util.SortedMap;

public class TimeSeriesOperations {

    private final IDocumentStore _store;
    private final String _database;
    private final MaintenanceOperationExecutor _executor;

    public TimeSeriesOperations(IDocumentStore store) {
        _store = store;
        _database = store.getDatabase();
        _executor = store.maintenance().forDatabase(_database);
    }

    private TimeSeriesOperations(IDocumentStore store, String database) {
        _store = store;
        _database = database;
        _executor = null;
    }

    /**
     * Register value names of a time-series
     * @param collectionClass Collection class
     * @param timeSeriesEntryClass Time-series entry class
     * @param <TCollection> Collection class
     * @param <TTimeSeriesEntry> Time-series entry class
     */
    public <TCollection, TTimeSeriesEntry> void register(Class<TCollection> collectionClass, Class<TTimeSeriesEntry> timeSeriesEntryClass) {
        register(collectionClass, timeSeriesEntryClass, null);
    }

    /**
     * Register value names of a time-series
     * @param collectionClass Collection class
     * @param timeSeriesEntryClass Time-series entry class
     * @param name Override time series entry name
     * @param <TCollection> Collection class
     * @param <TTimeSeriesEntry> Time-series entry class
     */
    public <TCollection, TTimeSeriesEntry> void register(Class<TCollection> collectionClass, Class<TTimeSeriesEntry> timeSeriesEntryClass, String name) {
        if (name == null) {
            name = getTimeSeriesName(timeSeriesEntryClass);
        }

        SortedMap<Byte, Tuple<Field, String>> mapping = TimeSeriesValuesHelper.getFieldsMapping(timeSeriesEntryClass);
        if (mapping == null) {
            throw new IllegalStateException(getTimeSeriesName(timeSeriesEntryClass) + " must contain " + TimeSeriesValue.class.getSimpleName());
        }

        String collection = _store.getConventions().getFindCollectionName().apply(collectionClass);
        String[] valueNames = mapping
                .values()
                .stream()
                .map(x -> x.second)
                .toArray(String[]::new);
        register(collection, name, valueNames);
    }

    /**
     * Register value name of a time-series
     * @param collectionClass Collection class
     * @param name Time series name
     * @param valueNames Values to register
     * @param <TCollection> Collection class
     */
    public <TCollection> void register(Class<TCollection> collectionClass, String name, String[] valueNames) {
        String collection = _store.getConventions().getFindCollectionName().apply(collectionClass);
        register(collection, name, valueNames);
    }

    /**
     * Register value names of a time-series
     * @param collection Collection name
     * @param name Time series name
     * @param valueNames Values to register
     */
    public void register(String collection, String name, String[] valueNames) {
        ConfigureTimeSeriesValueNamesOperation.Parameters parameters = new ConfigureTimeSeriesValueNamesOperation.Parameters();
        parameters.setCollection(collection);
        parameters.setTimeSeries(name);
        parameters.setValueNames(valueNames);
        parameters.setUpdate(true);

        ConfigureTimeSeriesValueNamesOperation command = new ConfigureTimeSeriesValueNamesOperation(parameters);
        _executor.send(command);
    }

    /**
     * Set rollup and retention policy
     * @param collectionClass Collection class
     * @param name Policy name
     * @param aggregation Aggregation time
     * @param retention Retention time
     * @param <TCollection> Collection class
     */
    public <TCollection> void setPolicy(Class<TCollection> collectionClass, String name, TimeValue aggregation, TimeValue retention) {
        String collection = _store.getConventions().getFindCollectionName().apply(collectionClass);
        setPolicy(collection, name, aggregation, retention);
    }

    /**
     * Set rollup and retention policy
     * @param collection Collection name
     * @param name Policy name
     * @param aggregation Aggregation time
     * @param retention Retention time
     */
    public void setPolicy(String collection, String name, TimeValue aggregation, TimeValue retention) {
        TimeSeriesPolicy p = new TimeSeriesPolicy(name, aggregation, retention);
        _executor.send(new ConfigureTimeSeriesPolicyOperation(collection, p));
    }

    /**
     * Set raw retention policy
     * @param collectionClass Collection class
     * @param retention Retention time
     * @param <TCollection> Collection class
     */
    public <TCollection> void setRawPolicy(Class<TCollection> collectionClass, TimeValue retention) {
        String collection = _store.getConventions().getFindCollectionName().apply(collectionClass);
        setRawPolicy(collection, retention);
    }

    /**
     * Set raw retention policy
     * @param collection Collection name
     * @param retention Retention time
     */
    public void setRawPolicy(String collection, TimeValue retention) {
        RawTimeSeriesPolicy p = new RawTimeSeriesPolicy(retention);
        _executor.send(new ConfigureRawTimeSeriesPolicyOperation(collection, p));
    }

    /**
     * Remove policy
     * @param collection Collection name
     * @param name Policy name
     */
    public void removePolicy(String collection, String name) {
        _executor.send(new RemoveTimeSeriesPolicyOperation(collection, name));
    }

    /**
     * Remove policy
     * @param clazz Collection class
     * @param name Policy name
     * @param <TCollection> Collection class
     */
    public <TCollection> void removePolicy(Class<TCollection> clazz, String name) {
        String collection = _store.getConventions().getFindCollectionName().apply(clazz);
        removePolicy(collection, name);
    }

    public static <TTimeSeriesEntry> String getTimeSeriesName(Class<TTimeSeriesEntry> clazz) {
        return clazz.getSimpleName();
    }

    public TimeSeriesOperations forDatabase(String database) {
        if (database.equalsIgnoreCase(_database)) {
            return this;
        }

        return new TimeSeriesOperations(_store, database);
    }
}
