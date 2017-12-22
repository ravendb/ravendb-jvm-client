package net.ravendb.client.exceptions.documents.revisions;

import net.ravendb.client.exceptions.RavenException;

public class RevisionsDisabledException extends RavenException {
    public RevisionsDisabledException() {
        super("Revisions are disabled");
    }

    public RevisionsDisabledException(String message) {
        super(message);
    }
}
