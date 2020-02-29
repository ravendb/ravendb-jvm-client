package net.ravendb.client.http;

import net.ravendb.client.documents.conventions.DocumentConventions;

public interface IBroadcast {
    IBroadcast prepareToBroadcast(DocumentConventions conventions);
}
