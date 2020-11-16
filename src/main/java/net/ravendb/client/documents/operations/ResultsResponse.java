package net.ravendb.client.documents.operations;

import net.ravendb.client.documents.indexes.IndexDefinition;
import net.ravendb.client.documents.indexes.IndexStats;
import net.ravendb.client.documents.indexes.PutIndexResult;
import net.ravendb.client.serverwide.operations.certificates.CertificateDefinition;
import net.ravendb.client.serverwide.operations.certificates.CertificateMetadata;
import net.ravendb.client.serverwide.operations.certificates.CertificateRawData;
import net.ravendb.client.serverwide.operations.configuration.ServerWideBackupConfiguration;

public class ResultsResponse<T> {

    private T[] results;

    public T[] getResults() {
        return results;
    }

    public void setResults(T[] results) {
        this.results = results;
    }

    public static class GetIndexNamesResponse extends ResultsResponse<String> {

    }

    public static class PutIndexesResponse extends ResultsResponse<PutIndexResult> {

    }

    public static class GetIndexesResponse extends ResultsResponse<IndexDefinition> {

    }

    public static class GetIndexStatisticsResponse extends ResultsResponse<IndexStats> {

    }

    public static class GetCertificatesResponse extends ResultsResponse<CertificateDefinition> {

    }

    public static class GetCertificatesMetadataResponse extends ResultsResponse<CertificateMetadata> {

    }

    public static class GetClientCertificatesResponse extends ResultsResponse<CertificateRawData> {

    }

    public static class GetServerWideBackupConfigurationsResponse extends ResultsResponse<ServerWideBackupConfiguration> {

    }
}
