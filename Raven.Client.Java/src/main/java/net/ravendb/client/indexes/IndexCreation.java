package net.ravendb.client.indexes;

import net.ravendb.abstractions.data.IndexStats;
import net.ravendb.abstractions.data.IndexToAdd;
import net.ravendb.client.document.DocumentConvention;

import java.util.ArrayList;
import java.util.List;

public class IndexCreation {

    public static IndexToAdd[] createIndexesToAdd(List<AbstractIndexCreationTask> indexCreationTasks, DocumentConvention conventions) {
        List<IndexToAdd> indexesToAdd = new ArrayList<>();
        for (AbstractIndexCreationTask creationTask : indexCreationTasks) {
            creationTask.setConventions(conventions);
            IndexToAdd indexToAdd = new IndexToAdd();
            indexToAdd.setDefinition(creationTask.createIndexDefinition());
            indexToAdd.setName(creationTask.getIndexName());
            indexToAdd.setPriority(creationTask.getPriority() != null ? creationTask.getPriority() : IndexStats.IndexingPriority.NORMAL);
            indexesToAdd.add(indexToAdd);
        }

        return indexesToAdd.toArray(new IndexToAdd[0]);
    }
}
