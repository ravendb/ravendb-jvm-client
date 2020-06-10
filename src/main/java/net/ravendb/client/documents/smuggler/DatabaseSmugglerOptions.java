package net.ravendb.client.documents.smuggler;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import net.ravendb.client.extensions.JsonExtensions;

import java.util.EnumSet;

public class DatabaseSmugglerOptions implements IDatabaseSmugglerOptions {

    public final EnumSet<DatabaseItemType> DEFAULT_OPERATE_ON_TYPES = EnumSet.of(DatabaseItemType.INDEXES, DatabaseItemType.DOCUMENTS,
            DatabaseItemType.REVISION_DOCUMENTS, DatabaseItemType.CONFLICTS, DatabaseItemType.DATABASE_RECORD, DatabaseItemType.IDENTITIES,
            DatabaseItemType.COMPARE_EXCHANGE, DatabaseItemType.ATTACHMENTS, DatabaseItemType.COUNTER_GROUPS, DatabaseItemType.SUBSCRIPTIONS,
            DatabaseItemType.TIME_SERIES);

    public final EnumSet<DatabaseRecordItemType> DEFAULT_OPERATE_ON_DATABASE_RECORD_TYPES = EnumSet.of(
            DatabaseRecordItemType.CLIENT,
            DatabaseRecordItemType.EXPIRATION,
            DatabaseRecordItemType.EXTERNAL_REPLICATIONS,
            DatabaseRecordItemType.PERIODIC_BACKUPS,
            DatabaseRecordItemType.RAVEN_CONNECTION_STRINGS,
            DatabaseRecordItemType.RAVEN_ETLS,
            DatabaseRecordItemType.REVISIONS,
            DatabaseRecordItemType.SETTINGS,
            DatabaseRecordItemType.SQL_CONNECTION_STRINGS,
            DatabaseRecordItemType.SORTERS,
            DatabaseRecordItemType.SQL_ETLS,
            DatabaseRecordItemType.HUB_PULL_REPLICATIONS,
            DatabaseRecordItemType.SINK_PULL_REPLICATIONS,
            DatabaseRecordItemType.TIME_SERIES,
            DatabaseRecordItemType.DOCUMENTS_COMPRESSION);

    private final int DEFAULT_MAX_STEPS_FOR_TRANSFORM_SCRIPT = 10 * 1000;

    private EnumSet<DatabaseItemType> operateOnTypes;
    private EnumSet<DatabaseRecordItemType> operateOnDatabaseRecordType;
    private boolean includeExpired;
    private boolean includeArtificial;
    private boolean removeAnalyzers;
    private String transformScript;
    private int maxStepsForTransformScript;
    private boolean skipRevisionCreation;
    private String encryptionKey;

    public DatabaseSmugglerOptions() {
        this.operateOnTypes = DEFAULT_OPERATE_ON_TYPES.clone();
        this.operateOnDatabaseRecordType = DEFAULT_OPERATE_ON_DATABASE_RECORD_TYPES.clone();
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

    @JsonSerialize(using = JsonExtensions.SharpEnumSetSerializer.class)
    public EnumSet<DatabaseRecordItemType> getOperateOnDatabaseRecordType() {
        return operateOnDatabaseRecordType;
    }

    public void setOperateOnDatabaseRecordType(EnumSet<DatabaseRecordItemType> operateOnDatabaseRecordType) {
        this.operateOnDatabaseRecordType = operateOnDatabaseRecordType;
    }

    public boolean isIncludeExpired() {
        return includeExpired;
    }

    public void setIncludeExpired(boolean includeExpired) {
        this.includeExpired = includeExpired;
    }

    public boolean isIncludeArtificial() {
        return includeArtificial;
    }

    public void setIncludeArtificial(boolean includeArtificial) {
        this.includeArtificial = includeArtificial;
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

    public boolean isSkipRevisionCreation() {
        return skipRevisionCreation;
    }

    public void setSkipRevisionCreation(boolean skipRevisionCreation) {
        this.skipRevisionCreation = skipRevisionCreation;
    }

    public String getEncryptionKey() {
        return encryptionKey;
    }

    public void setEncryptionKey(String encryptionKey) {
        this.encryptionKey = encryptionKey;
    }
}
