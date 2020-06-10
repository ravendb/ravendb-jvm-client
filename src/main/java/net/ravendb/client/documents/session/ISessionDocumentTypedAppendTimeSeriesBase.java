package net.ravendb.client.documents.session;

import net.ravendb.client.documents.session.timeSeries.TypedTimeSeriesEntry;

import java.util.Date;

public interface ISessionDocumentTypedAppendTimeSeriesBase<T> {
    void append(Date timestamp, T entry);
    void append(Date timestamp, T entry, String tag);
    void append(TypedTimeSeriesEntry<T> entry);
}
