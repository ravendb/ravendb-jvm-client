package net.ravendb.client.documents.smuggler;

import java.util.EnumSet;

public interface IDatabaseSmugglerOptions {
    EnumSet<DatabaseItemType> getOperateOnTypes();

    void setOperateOnTypes(EnumSet<DatabaseItemType> operateOnTypes);

    boolean isIncludeExpired();

    void setIncludeExpired(boolean includeExpired);

    boolean isRemoveAnalyzers();

    void setRemoveAnalyzers(boolean removeAnalyzers);

    String getTransformScript();

    void setTransformScript(String transformScript);

    int getMaxStepsForTransformScript();

    void setMaxStepsForTransformScript(int maxStepsForTransformScript);

    boolean isSkipRevisionCreation();

    void setSkipRevisionCreation(boolean skipRevisionCreation);
}
