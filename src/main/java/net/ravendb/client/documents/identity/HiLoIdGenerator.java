package net.ravendb.client.documents.identity;

import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.Lazy;
import net.ravendb.client.documents.commands.HiLoReturnCommand;
import net.ravendb.client.documents.commands.NextHiLoCommand;
import net.ravendb.client.http.RequestExecutor;

import java.util.Date;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 *  Generate HiLo numbers against a RavenDB document
 */
public class HiLoIdGenerator {

    private final IDocumentStore _store;
    private final String _tag;
    protected String prefix;
    private long _lastBatchSize;
    private Date _lastRangeDate;
    private final String _dbName;
    private final char _identityPartsSeparator;
    private volatile RangeValue _range;
    /**
     * @deprecated Will be removed in next major version of the product. Use field Range.ServerTag instead.
     */
    protected String serverTag;

    private AtomicReference<Lazy<Void>> _nextRangeTask = new AtomicReference<>(new Lazy<>(() -> null));

    public HiLoIdGenerator(String tag, IDocumentStore store, String dbName, char identityPartsSeparator) {
        _store = store;
        _tag = tag;
        _dbName = dbName;
        _identityPartsSeparator = identityPartsSeparator;
        _range = new RangeValue(1, 0, null);
    }

    /**
     * @deprecated Will be removed in next major version of the product. Use the getDocumentIdFromId(NextId) overload.
     * @param nextId next id
     * @return next id;
     */
    protected String getDocumentIdFromId(long nextId) {
        return prefix + nextId + "-" + serverTag;
    }

    public RangeValue getRange() {
        return _range;
    }

    public void setRange(RangeValue _range) {
        this._range = _range;
    }

    protected static class RangeValue {
        public final long Min;
        public final long Max;
        public final String ServerTag;
        public final AtomicLong Current;

        /**
         * @deprecated Will be removed in next major version of the product. Use RangeValue(min, max, serverTag) instead.
         */
        public RangeValue(long min, long max) {
            this(min, max, null);
        }

        public RangeValue(long min, long max, String serverTag) {
            Min = min;
            Max = max;
            Current = new AtomicLong(min - 1);
            ServerTag = serverTag;
        }
    }

    /**
     * Generates the document ID.
     * @param entity Entity
     * @return document id
     */
    public String generateDocumentId(Object entity) {
        NextId result = getNextId();
        return getDocumentIdFromId(result.getId());
    }

    public NextId getNextId() {
        while (true) {
            Lazy<Void> current = _nextRangeTask.get();

            // local range is not exhausted yet
            RangeValue range = _range;

            long id = range.Current.incrementAndGet();
            if (id <= range.Max) {
                return NextId.create(id, range.ServerTag);
            }

            try {
                // let's try to call the existing task for next range
                current.getValue();
                if (range != _range) {
                    continue;
                }
            } catch (Exception e) {
                // previous task was faulted, we will try to replace it
            }

            // local range is exhausted , need to get a new range
            Lazy<Void> maybeNextTask = new Lazy<>(this::getNextRange);
            boolean changed = _nextRangeTask.compareAndSet(current, maybeNextTask);
            if (changed) {
                maybeNextTask.getValue();
                continue;
            }

            try {
                // failed to replace, let's wait on the previous task
                _nextRangeTask.get().getValue();
            } catch (Exception e) {
                // previous task was faulted, we will try again
            }
        }
    }

    public long nextId() {
        NextId result = getNextId();
        return result.getId();
    }

    private Void getNextRange() {
        NextHiLoCommand hiloCommand = new NextHiLoCommand(_tag, _lastBatchSize, _lastRangeDate, _identityPartsSeparator, _range.Max);

        RequestExecutor re = _store.getRequestExecutor(_dbName);
        re.execute(hiloCommand);

        prefix = hiloCommand.getResult().getPrefix();
        serverTag = hiloCommand.getResult().getServerTag();
        _lastRangeDate = hiloCommand.getResult().getLastRangeAt();
        _lastBatchSize = hiloCommand.getResult().getLastSize();
        _range = new RangeValue(hiloCommand.getResult().getLow(), hiloCommand.getResult().getHigh(), hiloCommand.getResult().getServerTag());
        return null;
    }

    public void returnUnusedRange() {
        HiLoReturnCommand returnCommand = new HiLoReturnCommand(_tag, _range.Current.get(), _range.Max);

        RequestExecutor re = _store.getRequestExecutor(_dbName);
        re.execute(returnCommand);
    }

}
