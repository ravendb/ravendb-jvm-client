package net.ravendb.client.documents.operations.etl;

public class RavenEtlConfiguration extends EtlConfiguration<RavenConnectionString> {

    private String destination;
    private Integer loadRequestTimeoutInSec;

    @Override
    public String getDestination() {
        if (destination == null) {
            String joined = String.join(",", this.getConnection().getTopologyDiscoveryUrls());
            destination = this.getConnection().getDatabase() + "@" + joined;
        }
        return destination;

    }

    @Override
    public String getDefaultTaskName() {
        return "RavenDB ETL to " + this.getConnectionStringName();
    }

    @Override
    public boolean usingEncryptedCommunicationChannel() {

        for (String url : this.getConnection().getTopologyDiscoveryUrls()) {
            if (url.regionMatches(true, 0, "http:", 0, "http:".length())) {
                return false;
            }
        }

        return true;
    }

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
