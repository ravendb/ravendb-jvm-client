package net.ravendb.client.documents.session;

import net.ravendb.client.documents.session.timeSeries.TypedTimeSeriesRollupEntry;

import java.util.Date;
import java.util.List;

public interface ISessionDocumentRollupTypedTimeSeries<TValues>
        extends ISessionDocumentRollupTypedAppendTimeSeriesBase<TValues>,
        ISessionDocumentRemoveTimeSeriesBase {
    List<TypedTimeSeriesRollupEntry<TValues>> get();
    List<TypedTimeSeriesRollupEntry<TValues>> get(Date from, Date to);
    List<TypedTimeSeriesRollupEntry<TValues>> get(Date from, Date to, int start);

    List<TypedTimeSeriesRollupEntry<TValues>> get(Date from, Date to, int start, int pageSize);
}
