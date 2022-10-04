package net.ravendb.client.documents.session;

import java.util.Date;

public interface ISessionDocumentIncrementTimeSeriesBase {
    void increment(Date timestamp, double[] values);
    void increment(double[] values);
    void increment(Date timestamp, double value);
    void increment(double value);
}
