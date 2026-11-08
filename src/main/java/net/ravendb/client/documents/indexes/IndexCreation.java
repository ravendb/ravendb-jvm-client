package net.ravendb.client.documents.indexes;

import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.conventions.DocumentConventions;
import org.apache.commons.lang3.ObjectUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class IndexCreation {

    public static void createIndexes(Collection<? extends IAbstractIndexCreationTask> indexes, IDocumentStore store) {
        createIndexes(indexes, store, null);
    }

    public static void createIndexes(Collection<? extends IAbstractIndexCreationTask> indexes, IDocumentStore store, DocumentConventions conventions) {
        createIndexes(indexes, store, conventions, null);
    }

    public static void createIndexes(Collection<? extends IAbstractIndexCreationTask> indexes, IDocumentStore store, DocumentConventions conventions, String database) {
        List<IAbstractIndexCreationTask> indexesList = indexes != null ? new ArrayList<>(indexes) : new ArrayList<>();

        store.executeIndexes(indexesList, conventions, database);
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
