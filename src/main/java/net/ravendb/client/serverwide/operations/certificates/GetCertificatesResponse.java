package net.ravendb.client.serverwide.operations.certificates;

import java.util.List;

public class GetCertificatesResponse {
    private List<CertificateDefinition> results;

    public List<CertificateDefinition> getResults() {
        return results;
    }

    public void setResults(List<CertificateDefinition> results) {
        this.results = results;
    }
}
