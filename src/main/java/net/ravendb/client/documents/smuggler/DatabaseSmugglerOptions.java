package net.ravendb.client.documents.smuggler;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import net.ravendb.client.extensions.JsonExtensions;

import java.util.EnumSet;

public class DatabaseSmugglerOptions implements IDatabaseSmugglerOptions {

    public final EnumSet<DatabaseItemType> DEFAULT_OPERATE_ON_TYPES = EnumSet.of(DatabaseItemType.INDEXES, DatabaseItemType.DOCUMENTS,
            DatabaseItemType.REVISION_DOCUMENTS, DatabaseItemType.CONFLICTS, DatabaseItemType.DATABASE_RECORD, DatabaseItemType.IDENTITIES,
            DatabaseItemType.COMPARE_EXCHANGE, DatabaseItemType.ATTACHMENTS, DatabaseItemType.COUNTERS);

    private final int DEFAULT_MAX_STEPS_FOR_TRANSFORM_SCRIPT = 10 * 1000;

    private EnumSet<DatabaseItemType> operateOnTypes;
    private boolean includeExpired;
    private boolean removeAnalyzers;
    private String transformScript;
    private int maxStepsForTransformScript;
    private boolean skipRevisionCreation;

    public DatabaseSmugglerOptions() {
        this.operateOnTypes = DEFAULT_OPERATE_ON_TYPES.clone();
        this.maxStepsForTransformScript = DEFAULT_MAX_STEPS_FOR_TRANSFORM_SCRIPT;
        includeExpired = true;
    }

    @JsonSerialize(using = JsonExtensions.SharpEnumSetSerializer.class)
    public EnumSet<DatabaseItemType> getOperateOnTypes() {
        return operateOnTypes;
    }

    public void setOperateOnTypes(EnumSet<DatabaseItemType> operateOnTypes) {
        this.operateOnTypes = operateOnTypes;
    }

    public boolean isIncludeExpired() {
        return includeExpired;
    }

    public void setIncludeExpired(boolean includeExpired) {
        this.includeExpired = includeExpired;
    }

    public boolean isRemoveAnalyzers() {
        return removeAnalyzers;
    }

    public void setRemoveAnalyzers(boolean removeAnalyzers) {
        this.removeAnalyzers = removeAnalyzers;
    }

    public String getTransformScript() {
        return transformScript;
    }

    public void setTransformScript(String transformScript) {
        this.transformScript = transformScript;
    }

    public int getMaxStepsForTransformScript() {
        return maxStepsForTransformScript;
    }

    public void setMaxStepsForTransformScript(int maxStepsForTransformScript) {
        this.maxStepsForTransformScript = maxStepsForTransformScript;
    }

    @Deprecated
    public boolean isSkipRevisionCreation() {
        return skipRevisionCreation;
    }

    @Deprecated
    public void setSkipRevisionCreation(boolean skipRevisionCreation) {
        this.skipRevisionCreation = skipRevisionCreation;
    }

}
