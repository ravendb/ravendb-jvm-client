package net.ravendb.client.documents.identity;

import net.ravendb.client.documents.DocumentStore;
import net.ravendb.client.documents.conventions.DocumentConventions;
import org.apache.commons.lang3.StringUtils;

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

    public MultiTypeHiLoIdGenerator(DocumentStore store, String dbName, DocumentConventions conventions) {
        this.store = store;
        this.dbName = dbName;
        this.conventions = conventions;
    }

    public String generateDocumentId(Object entity) {
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

    protected HiLoIdGenerator createGeneratorFor(String tag) {
        return new HiLoIdGenerator(tag, store, dbName, conventions.getIdentityPartsSeparator());
    }

    public void returnUnusedRange() {
        for (HiLoIdGenerator generator : _idGeneratorsByTag.values()) {
            generator.returnUnusedRange();
        }
    }

}
