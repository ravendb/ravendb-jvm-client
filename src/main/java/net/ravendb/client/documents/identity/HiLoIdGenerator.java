package net.ravendb.client.documents.identity;

import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.commands.HiLoReturnCommand;
import net.ravendb.client.documents.commands.NextHiLoCommand;
import net.ravendb.client.http.RequestExecutor;

import java.util.Date;
import java.util.concurrent.atomic.AtomicLong;

/**
 *  Generate hilo numbers against a RavenDB document
 */
public class HiLoIdGenerator {

    private final Object generatorLock = new Object();

    private final IDocumentStore _store;
    private final String _tag;
    protected String prefix;
    private long _lastBatchSize;
    private Date _lastRangeDate;
    private final String _dbName;
    private final String _identityPartsSeparator;
    private volatile RangeValue _range;
    protected String serverTag;

    public HiLoIdGenerator(String tag, IDocumentStore store, String dbName, String identityPartsSeparator) {
        _store = store;
        _tag = tag;
        _dbName = dbName;
        _identityPartsSeparator = identityPartsSeparator;
        _range = new RangeValue(1, 0);
    }

    protected String getDocumentIdFromId(long nextId) {
        return prefix + nextId + "-" + serverTag;
    }

    public RangeValue getRange() {
        return _range;
    }

    public void setRange(RangeValue _range) {
        this._range = _range;
    }

    protected class RangeValue {
        public final long Min;
        public final long Max;
        public final AtomicLong Current;

        public RangeValue(long min, long max) {
            Min = min;
            Max = max;
            Current = new AtomicLong(min - 1);
        }
    }

    /**
     * Generates the document ID.
     * @param entity Entity
     * @return document id
     */
    public String generateDocumentId(Object entity) {
        return getDocumentIdFromId(nextId());
    }

    public long nextId() {
        while (true) {
            // local range is not exhausted yet
            RangeValue range = _range;

            long id = range.Current.incrementAndGet();
            if (id <= range.Max) {
                return id;
            }

            //local range is exhausted , need to get a new range
            synchronized (generatorLock) {
                id = _range.Current.get();
                if (id <= _range.Max) {
                    return id;
                }

                getNextRange();
            }
        }
    }

    private void getNextRange() {
        NextHiLoCommand hiloCommand = new NextHiLoCommand(_tag, _lastBatchSize, _lastRangeDate, _identityPartsSeparator, _range.Max);

        RequestExecutor re = _store.getRequestExecutor(_dbName);
        re.execute(hiloCommand);

        prefix = hiloCommand.getResult().getPrefix();
        serverTag = hiloCommand.getResult().getServerTag();
        _lastRangeDate = hiloCommand.getResult().getLastRangeAt();
        _lastBatchSize = hiloCommand.getResult().getLastSize();
        _range = new RangeValue(hiloCommand.getResult().getLow(), hiloCommand.getResult().getHigh());
    }

    public void returnUnusedRange() {
        HiLoReturnCommand returnCommand = new HiLoReturnCommand(_tag, _range.Current.get(), _range.Max);

        RequestExecutor re = _store.getRequestExecutor(_dbName);
        re.execute(returnCommand);
    }

}
