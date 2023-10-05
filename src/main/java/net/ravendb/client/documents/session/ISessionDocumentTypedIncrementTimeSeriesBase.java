package net.ravendb.client.documents.session;

import java.util.Date;

public interface ISessionDocumentTypedIncrementTimeSeriesBase<T> {
    void increment(Date timestamp, T entry);

    void increment(T entry);
}
