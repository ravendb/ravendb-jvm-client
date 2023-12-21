package net.ravendb.client.documents.smuggler;

import java.util.EnumSet;
import java.util.List;

public interface IDatabaseSmugglerOptions {
    EnumSet<DatabaseRecordItemType> getOperateOnDatabaseRecordType();

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
}
