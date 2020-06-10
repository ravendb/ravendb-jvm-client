package net.ravendb.client.documents.session;

import net.ravendb.client.documents.session.timeSeries.TypedTimeSeriesRollupEntry;

public interface ISessionDocumentRollupTypedAppendTimeSeriesBase<T> {
    void append(TypedTimeSeriesRollupEntry<T> entry);
}
