package net.ravendb.client.documents.operations.etl;

public class RavenEtlConfiguration extends EtlConfiguration<RavenConnectionString> {

    private Integer loadRequestTimeoutInSec;

    public EtlType getEtlType() {
        return EtlType.RAVEN;
    }

    public Integer getLoadRequestTimeoutInSec() {
        return loadRequestTimeoutInSec;
    }

    public void setLoadRequestTimeoutInSec(Integer loadRequestTimeoutInSec) {
        this.loadRequestTimeoutInSec = loadRequestTimeoutInSec;
    }
}
