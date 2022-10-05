package net.ravendb.client.documents.session;

import net.ravendb.client.documents.operations.BulkInsertProgress;
import net.ravendb.client.primitives.EventArgs;

public class BulkInsertOnProgressEventArgs extends EventArgs {

    private final BulkInsertProgress progress;

    public BulkInsertOnProgressEventArgs(BulkInsertProgress progress) {
        this.progress = progress;
    }

    public BulkInsertProgress getProgress() {
        return progress;
    }
}
