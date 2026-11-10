package net.ravendb.client.documents.identity;

import net.ravendb.client.documents.DocumentStore;
import net.ravendb.client.documents.conventions.DocumentConventions;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 *  Generate a hilo ID for each given type
 */
public class MultiTypeHiLoIdGenerator {

    private final Object _generatorLock = new Object();
    private final ConcurrentMap<String, HiLoIdGenerator> _idGeneratorsByTag = new ConcurrentHashMap<>();
    protected final DocumentStore store;
    protected final String dbName;
    protected final DocumentConventions conventions;
    private char _identityPartsSeparator;

    public MultiTypeHiLoIdGenerator(DocumentStore store, String dbName) {
        this.store = store;
        this.dbName = dbName;
        this.conventions = store.getRequestExecutor(dbName).getConventions();
        _identityPartsSeparator = conventions.getIdentityPartsSeparator();
    }

    /**
     * @param entity Entity
     * @return document id
     * @deprecated This method is deprecated and will be removed in RavenDB 8.0, use the overload with collectionName instead
     */
    @Deprecated
    public String generateDocumentId(Object entity) {
        char identityPartsSeparator = conventions.getIdentityPartsSeparator();
        if (_identityPartsSeparator != identityPartsSeparator) {
            maybeRefresh(identityPartsSeparator);
        }

        String typeTagName = conventions.getCollectionName(entity);

        if (StringUtils.isEmpty(typeTagName)) {
            return null;
        }

        String tag = conventions.getTransformClassCollectionNameToDocumentIdPrefix().apply(typeTagName);

        HiLoIdGenerator value = _idGeneratorsByTag.get(tag);
        if (value != null) {
            return value.generateDocumentId(entity);
        }

        synchronized (_generatorLock) {
            value = _idGeneratorsByTag.get(tag);

            if (value != null) {
                return value.generateDocumentId(entity);
            }

            value = createGeneratorFor(tag);
            _idGeneratorsByTag.put(tag, value);
        }

        return value.generateDocumentId(entity);
    }

    public String generateDocumentId(String collectionName) {
        char identityPartsSeparator = conventions.getIdentityPartsSeparator();
        if (_identityPartsSeparator != identityPartsSeparator) {
            maybeRefresh(identityPartsSeparator);
        }

        if (StringUtils.isEmpty(collectionName)) {
            return null;
        }

        String tag = conventions.getTransformClassCollectionNameToDocumentIdPrefix().apply(collectionName);

        HiLoIdGenerator value = _idGeneratorsByTag.get(tag);
        if (value != null) {
            return value.generateDocumentId(collectionName);
        }

        synchronized (_generatorLock) {
            value = _idGeneratorsByTag.get(tag);

            if (value != null) {
                return value.generateDocumentId(collectionName);
            }

            value = createGeneratorFor(tag);
            _idGeneratorsByTag.put(tag, value);
        }

        return value.generateDocumentId(collectionName);
    }

    private void maybeRefresh(char identityPartsSeparator) {
        List<HiLoIdGenerator> idGenerators = null;

        synchronized (_generatorLock) {
            if (_identityPartsSeparator == identityPartsSeparator) {
                return;
            }

            idGenerators = new ArrayList<>(_idGeneratorsByTag.values());

            _idGeneratorsByTag.clear();
            _identityPartsSeparator = identityPartsSeparator;
        }

        if (idGenerators != null) {
            try {
                returnUnusedRange(idGenerators);
            } catch (Exception e) {
                // ignored
            }
        }
    }

    public long generateNextIdFor(String collectionName) {
        collectionName = conventions.getTransformClassCollectionNameToDocumentIdPrefix().apply(collectionName);

        HiLoIdGenerator value = _idGeneratorsByTag.get(collectionName);
        if (value != null) {
            return value.getNextId().getId();
        }

        synchronized (_generatorLock) {
            value = _idGeneratorsByTag.get(collectionName);
            if (value != null) {
                return value.getNextId().getId();
            }

            value = createGeneratorFor(collectionName);
            _idGeneratorsByTag.put(collectionName, value);
        }

        return value.getNextId().getId();
    }

    protected HiLoIdGenerator createGeneratorFor(String tag) {
        return new HiLoIdGenerator(tag, store, dbName, _identityPartsSeparator);
    }

    public void returnUnusedRange() {
        returnUnusedRange(_idGeneratorsByTag.values());
    }

    private static void returnUnusedRange(Collection<HiLoIdGenerator> generators) {
        for (HiLoIdGenerator generator : generators) {
            generator.returnUnusedRange();
        }
    }

}
