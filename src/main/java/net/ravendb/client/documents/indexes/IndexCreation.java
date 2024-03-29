package net.ravendb.client.documents.indexes;

import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.operations.indexes.PutIndexesOperation;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Collection;

public class IndexCreation {
    private static final Log logger = LogFactory.getLog(IndexCreation.class);

    public static void createIndexes(Collection<? extends IAbstractIndexCreationTask> indexes, IDocumentStore store) {
        createIndexes(indexes, store, null);
    }

    public static void createIndexes(Collection<? extends IAbstractIndexCreationTask> indexes, IDocumentStore store, DocumentConventions conventions) {

        if (conventions == null) {
            conventions = store.getConventions();
        }

        try {
            IndexDefinition[] indexesToAdd = createIndexesToAdd(indexes, conventions);
            store.maintenance().send(new PutIndexesOperation(indexesToAdd));
        } catch (Exception e) { // For old servers that don't have the new endpoint for executing multiple indexes
            logger.info("Could not create indexes in one shot (maybe using older version of RavenDB ?)", e);

            for (IAbstractIndexCreationTask index : indexes) {
                index.execute(store, conventions);
            }
        }
    }

    public static IndexDefinition[] createIndexesToAdd(Collection<? extends IAbstractIndexCreationTask> indexCreationTasks, DocumentConventions conventions) {
        return indexCreationTasks.stream()
                .map(x -> {
                    DocumentConventions oldConventions = x.getConventions();

                    try {
                        x.setConventions(conventions);
                        IndexDefinition definition = x.createIndexDefinition();
                        definition.setName(x.getIndexName());
                        definition.setPriority(ObjectUtils.firstNonNull(x.getPriority(), IndexPriority.NORMAL));
                        definition.setState(ObjectUtils.firstNonNull(x.getState(), IndexState.NORMAL));
                        return definition;
                    } finally {
                        x.setConventions(oldConventions);
                    }
                }).toArray(IndexDefinition[]::new);
    }
}
