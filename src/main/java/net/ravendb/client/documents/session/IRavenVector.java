package net.ravendb.client.documents.session;

import java.lang.Iterable;

public interface IRavenVector<T extends Number> extends Iterable<T> {
    // No additional methods; just a marker for type consistency
}