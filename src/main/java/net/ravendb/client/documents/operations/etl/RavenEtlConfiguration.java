package net.ravendb.client.documents.operations.etl;

public class RavenEtlConfiguration extends EtlConfiguration<RavenConnectionString> {

    private Integer loadRequestTimeoutInSec;

    public Integer getLoadRequestTimeoutInSec() {
        return loadRequestTimeoutInSec;
    }

    public void setLoadRequestTimeoutInSec(Integer loadRequestTimeoutInSec) {
        this.loadRequestTimeoutInSec = loadRequestTimeoutInSec;
    }

    @Override
    public EtlType getEtlType() { return EtlType.RAVEN; }
}
