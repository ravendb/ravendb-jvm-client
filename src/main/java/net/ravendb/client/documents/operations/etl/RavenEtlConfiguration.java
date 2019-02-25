package net.ravendb.client.documents.operations.etl;

import net.ravendb.client.serverwide.etl.RavenConnectionString;

public class RavenEtlConfiguration extends EtlConfiguration<RavenConnectionString> {

    private Integer loadRequestTimeoutInSec;

    public Integer getLoadRequestTimeoutInSec() {
        return loadRequestTimeoutInSec;
    }

    public void setLoadRequestTimeoutInSec(Integer loadRequestTimeoutInSec) {
        this.loadRequestTimeoutInSec = loadRequestTimeoutInSec;
    }
}
