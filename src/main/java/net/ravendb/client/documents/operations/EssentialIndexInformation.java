package net.ravendb.client.documents.operations;

import net.ravendb.client.documents.dataArchival.ArchivedDataProcessingBehavior;
import net.ravendb.client.documents.indexes.IndexLockMode;
import net.ravendb.client.documents.indexes.IndexPriority;
import net.ravendb.client.documents.indexes.IndexSourceType;
import net.ravendb.client.documents.indexes.IndexType;

public class EssentialIndexInformation {
    private String name;
    private IndexLockMode lockMode;
    private IndexPriority priority;
    private IndexType type;
    private IndexSourceType sourceType;
    private ArchivedDataProcessingBehavior archivedDataProcessingBehavior;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public IndexLockMode getLockMode() {
        return lockMode;
    }

    public void setLockMode(IndexLockMode lockMode) {
        this.lockMode = lockMode;
    }

    public IndexPriority getPriority() {
        return priority;
    }

    public void setPriority(IndexPriority priority) {
        this.priority = priority;
    }

    public IndexType getType() {
        return type;
    }

    public void setType(IndexType type) {
        this.type = type;
    }

    public IndexSourceType getSourceType() {
        return sourceType;
    }

    public void setSourceType(IndexSourceType sourceType) {
        this.sourceType = sourceType;
    }

    public ArchivedDataProcessingBehavior getArchivedDataProcessingBehavior() {
        return archivedDataProcessingBehavior;
    }

    public void setArchivedDataProcessingBehavior(ArchivedDataProcessingBehavior archivedDataProcessingBehavior) {
        this.archivedDataProcessingBehavior = archivedDataProcessingBehavior;
    }
}
