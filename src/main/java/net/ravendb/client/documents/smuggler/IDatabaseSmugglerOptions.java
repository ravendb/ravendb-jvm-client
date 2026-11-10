package net.ravendb.client.documents.smuggler;

import java.util.EnumSet;
import java.util.List;

public interface IDatabaseSmugglerOptions {
    EnumSet<DatabaseRecordItemType> getOperateOnDatabaseRecordTypes();

    void setOperateOnDatabaseRecordTypes(EnumSet<DatabaseRecordItemType> operateOnDatabaseRecordTypes);

    /**
     * @deprecated Use {@link #getOperateOnDatabaseRecordTypes()} instead.
     * @return database record item types
     */
    @Deprecated
    EnumSet<DatabaseRecordItemType> getOperateOnDatabaseRecordType();

    /**
     * @deprecated Use {@link #setOperateOnDatabaseRecordTypes(EnumSet)} instead.
     * @param operateOnDatabaseRecordType database record item types
     */
    @Deprecated
    void setOperateOnDatabaseRecordType(EnumSet<DatabaseRecordItemType> operateOnDatabaseRecordType);

    EnumSet<DatabaseItemType> getOperateOnTypes();

    void setOperateOnTypes(EnumSet<DatabaseItemType> operateOnTypes);

    boolean isIncludeExpired();

    void setIncludeExpired(boolean includeExpired);

    boolean isIncludeArtificial();

    void setIncludeArtificial(boolean includeArtificial);

    boolean isIncludeArchived();

    void setIncludeArchived(boolean includeArchived);

    boolean isRemoveAnalyzers();

    void setRemoveAnalyzers(boolean removeAnalyzers);

    String getTransformScript();

    void setTransformScript(String transformScript);

    int getMaxStepsForTransformScript();

    void setMaxStepsForTransformScript(int maxStepsForTransformScript);

    boolean isSkipRevisionCreation();

    void setSkipRevisionCreation(boolean skipRevisionCreation);

    List<String> getCollections();

    void setCollections(List<String> collections);

    Integer getMaxReadOpsPerSecond();

    void setMaxReadOpsPerSecond(Integer maxReadOpsPerSecond);
}
