package net.ravendb.client.documents.operations.backups;

import com.fasterxml.jackson.annotation.JsonProperty;

public class AbstractGetPeriodicBackupStatusOperationResult {
    @JsonProperty("IsSharded")
    private boolean sharded;

    public boolean isSharded() {
        return sharded;
    }

    public void setSharded(boolean sharded) {
        this.sharded = sharded;
    }
}
