package net.ravendb.client.documents.smuggler;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import net.ravendb.client.extensions.JsonExtensions;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

public class DatabaseSmugglerOptions implements IDatabaseSmugglerOptions {

    public final EnumSet<DatabaseItemType> DEFAULT_OPERATE_ON_TYPES = EnumSet.of(
            DatabaseItemType.INDEXES,
            DatabaseItemType.DOCUMENTS,
            DatabaseItemType.REVISION_DOCUMENTS,
            DatabaseItemType.CONFLICTS,
            DatabaseItemType.DATABASE_RECORD,
            DatabaseItemType.REPLICATION_HUB_CERTIFICATES,
            DatabaseItemType.IDENTITIES,
            DatabaseItemType.COMPARE_EXCHANGE,
            DatabaseItemType.ATTACHMENTS,
            DatabaseItemType.COUNTER_GROUPS,
            DatabaseItemType.SUBSCRIPTIONS,
            DatabaseItemType.TIME_SERIES);

    public final EnumSet<DatabaseRecordItemType> DEFAULT_OPERATE_ON_DATABASE_RECORD_TYPES = EnumSet.of(
            DatabaseRecordItemType.CLIENT,
            DatabaseRecordItemType.CONFLICT_SOLVER_CONFIG,
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
            DatabaseRecordItemType.DOCUMENTS_COMPRESSION,
            DatabaseRecordItemType.ANALYZERS,
            DatabaseRecordItemType.LOCK_MODE,
            DatabaseRecordItemType.OLAP_CONNECTION_STRINGS,
            DatabaseRecordItemType.OLAP_ETLS,
            DatabaseRecordItemType.ELASTIC_SEARCH_CONNECTION_STRINGS,
            DatabaseRecordItemType.ELASTIC_SEARCH_ETLS,
            DatabaseRecordItemType.POSTGRE_SQL_INTEGRATION,
            DatabaseRecordItemType.QUEUE_CONNECTION_STRINGS,
            DatabaseRecordItemType.QUEUE_ETLS,
            DatabaseRecordItemType.INDEXES_HISTORY,
            DatabaseRecordItemType.REFRESH,
            DatabaseRecordItemType.QUEUE_SINKS,
            DatabaseRecordItemType.DATA_ARCHIVAL);

    private final int DEFAULT_MAX_STEPS_FOR_TRANSFORM_SCRIPT = 10 * 1000;

    private EnumSet<DatabaseItemType> operateOnTypes;
    private EnumSet<DatabaseRecordItemType> operateOnDatabaseRecordType;
    private boolean includeExpired;
    private boolean includeArtificial;
    private boolean includeArchived;
    private boolean removeAnalyzers;
    private String transformScript;
    private int maxStepsForTransformScript;
    private boolean skipRevisionCreation;
    private String encryptionKey;
    private List<String> collections;

    private boolean skipCorruptedData;

    public DatabaseSmugglerOptions() {
        this.operateOnTypes = DEFAULT_OPERATE_ON_TYPES.clone();
        this.operateOnDatabaseRecordType = DEFAULT_OPERATE_ON_DATABASE_RECORD_TYPES.clone();
        this.maxStepsForTransformScript = DEFAULT_MAX_STEPS_FOR_TRANSFORM_SCRIPT;
        includeExpired = true;
        includeArchived = true;
        this.collections = new ArrayList<>();
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

    public boolean isIncludeArchived() {
        return includeArchived;
    }

    public void setIncludeArchived(boolean includeArchived) {
        this.includeArchived = includeArchived;
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

    public List<String> getCollections() {
        return collections;
    }

    public void setCollections(List<String> collections) {
        this.collections = collections;
    }

    /**
     * In case the database is corrupted (for example, Compression Dictionaries are lost), it is possible to export all the remaining data.
     */
    public boolean isSkipCorruptedData() {
        return skipCorruptedData;
    }

    /**
     * In case the database is corrupted (for example, Compression Dictionaries are lost), it is possible to export all the remaining data.
     * @param skipCorruptedData skip corrupted data
     */
    public void setSkipCorruptedData(boolean skipCorruptedData) {
        this.skipCorruptedData = skipCorruptedData;
    }
}
