package net.ravendb.client.documents.session;

import net.ravendb.client.documents.session.timeSeries.TypedTimeSeriesRollupEntry;

import java.util.Date;

public interface ISessionDocumentRollupTypedTimeSeries<TValues>
        extends ISessionDocumentRollupTypedAppendTimeSeriesBase<TValues>,
        ISessionDocumentDeleteTimeSeriesBase {
    TypedTimeSeriesRollupEntry<TValues>[] get();
    TypedTimeSeriesRollupEntry<TValues>[] get(Date from, Date to);
    TypedTimeSeriesRollupEntry<TValues>[] get(Date from, Date to, int start);
    TypedTimeSeriesRollupEntry<TValues>[] get(Date from, Date to, int start, int pageSize);
}
