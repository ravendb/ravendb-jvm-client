package net.ravendb.client.documents.session;

import net.ravendb.client.documents.session.timeSeries.TypedTimeSeriesRollupEntry;

import java.util.Date;

public interface ISessionDocumentRollupTypedAppendTimeSeriesBase<T> {
    void append(Date timestamp, T entry);
    void append(Date timestamp, T entry, String tag);
    void append(TypedTimeSeriesRollupEntry<T> entry);
}
