package net.ravendb.client.serverwide.operations.builder;

import net.ravendb.client.documents.indexes.IndexDefinition;
import net.ravendb.client.documents.indexes.analysis.AnalyzerDefinition;
import net.ravendb.client.documents.operations.configuration.ClientConfiguration;
import net.ravendb.client.documents.operations.configuration.StudioConfiguration;
import net.ravendb.client.documents.operations.expiration.ExpirationConfiguration;
import net.ravendb.client.documents.operations.refresh.RefreshConfiguration;
import net.ravendb.client.documents.operations.revisions.RevisionsConfiguration;
import net.ravendb.client.documents.operations.timeSeries.TimeSeriesConfiguration;
import net.ravendb.client.documents.queries.sorting.SorterDefinition;
import net.ravendb.client.serverwide.DatabaseRecord;
import net.ravendb.client.serverwide.DocumentsCompressionConfiguration;

import java.util.Map;
import java.util.function.Consumer;

public interface IDatabaseRecordBuilderBase {
    DatabaseRecord toDatabaseRecord();
    IDatabaseRecordBuilderBase disabled();
    IDatabaseRecordBuilderBase encrypted();
    IDatabaseRecordBuilderBase withLockMode(DatabaseRecord.DatabaseLockMode lockMode);
    IDatabaseRecordBuilderBase configureDocumentsCompression(DocumentsCompressionConfiguration configuration);
    IDatabaseRecordBuilderBase withSorters(SorterDefinition... sorterDefinitions);
    IDatabaseRecordBuilderBase withAnalyzers(AnalyzerDefinition... analyzerDefinitions);
    IDatabaseRecordBuilderBase withIndexes(IndexDefinition... indexDefinitions);
    IDatabaseRecordBuilderBase withSettings(Map<String, String> settings);
    IDatabaseRecordBuilderBase withSettings(Consumer<Map<String, String>> builder);
    IDatabaseRecordBuilderBase configureRevisions(RevisionsConfiguration configuration);
    IDatabaseRecordBuilderBase withEtls(Consumer<IEtlConfigurationBuilder> builder);
    IDatabaseRecordBuilderBase withBackups(Consumer<IBackupConfigurationBuilder> builder);
    IDatabaseRecordBuilderBase withReplication(Consumer<IReplicationConfigurationBuilder> builder);
    IDatabaseRecordBuilderBase withConnectionStrings(Consumer<IConnectionStringConfigurationBuilder> builder);
    IDatabaseRecordBuilderBase configureClient(ClientConfiguration configuration);
    IDatabaseRecordBuilderBase configureStudio(StudioConfiguration configuration);
    IDatabaseRecordBuilderBase configureRefresh(RefreshConfiguration configuration);
    IDatabaseRecordBuilderBase configureExpiration(ExpirationConfiguration configuration);
    IDatabaseRecordBuilderBase configureTimeSeries(TimeSeriesConfiguration configuration);
    IDatabaseRecordBuilderBase withIntegrations(Consumer<IIntegrationConfigurationBuilder> builder);
}
