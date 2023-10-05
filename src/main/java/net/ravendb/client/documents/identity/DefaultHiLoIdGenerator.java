package net.ravendb.client.documents.identity;

import net.ravendb.client.documents.IDocumentStore;

public class DefaultHiLoIdGenerator extends HiLoIdGenerator {
    public DefaultHiLoIdGenerator(String tag, IDocumentStore store, String dbName, char identityPartsSeparator) {
        super(tag, store, dbName, identityPartsSeparator);
    }

    protected String getDocumentIdFromId(NextId result) {
        return prefix + result.getId() + "-" + result.getServerTag();
    }

    @Override
    public String generateDocumentId(Object entity) {
        NextId result = getNextId();
        return getDocumentIdFromId(result);
    }
}
