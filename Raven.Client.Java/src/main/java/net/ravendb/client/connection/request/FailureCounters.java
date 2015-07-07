package net.ravendb.client.connection.request;

import net.ravendb.abstractions.basic.EventHandler;
import net.ravendb.abstractions.basic.EventHelper;
import net.ravendb.client.connection.ReplicationInformer;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

public class FailureCounters {

    protected List<EventHandler<ReplicationInformer.FailoverStatusChangedEventArgs>> failoverStatusChanged = new ArrayList<>();

    public void addFailoverStatusChanged(EventHandler<ReplicationInformer.FailoverStatusChangedEventArgs> event) {
        failoverStatusChanged.add(event);
    }

    public void removeFailoverStatusChanged(EventHandler<ReplicationInformer.FailoverStatusChangedEventArgs> event) {
        failoverStatusChanged.remove(event);
    }

    protected final Map<String, FailureCounter> failureCounts = new ConcurrentHashMap<>();

    public AtomicLong getFailureCount(String operationUrl) {
        return getHolder(operationUrl).getValue();
    }

    public Date getFailureLastCheck(String operationUrl) {
        return getHolder(operationUrl).getLastCheck();
    }

    public FailureCounter getHolder(String operationUrl) {
        if (!failureCounts.containsKey(operationUrl)) {
            failureCounts.put(operationUrl, new FailureCounter());
        }
        return failureCounts.get(operationUrl);
    }

    public Map<String, FailureCounter> getFailureCounts() {
        return failureCounts;
    }

    public boolean isFirstFailure(String operationUrl) {
        FailureCounter value = getHolder(operationUrl);
        return value.getValue().longValue() == 0;
    }

    @SuppressWarnings("boxing")
    public void incrementFailureCount(String operationUrl) {
        FailureCounter value = getHolder(operationUrl);
        value.setForceCheck(false);
        long current = value.getValue().incrementAndGet();
        if (current == 1) { // first failure
            EventHelper.invoke(failoverStatusChanged, this, new ReplicationInformer.FailoverStatusChangedEventArgs(operationUrl, true));
        }
    }


    @SuppressWarnings("boxing")
    public void resetFailureCount(String operationUrl) {
        FailureCounter value = getHolder(operationUrl);
        long oldVal = value.getValue().getAndSet(0);
        value.setLastCheck(new Date());
        value.setForceCheck(false);
        if (oldVal != 0) {
            EventHelper.invoke(failoverStatusChanged, this, new ReplicationInformer.FailoverStatusChangedEventArgs(operationUrl, false));
        }
    }

    public void forceCheck(String primaryUrl, boolean shouldForceCheck) {
        FailureCounter failureCounter = getHolder(primaryUrl);
        failureCounter.setForceCheck(shouldForceCheck);
    }

    public static class FailureCounter {

        private AtomicLong value = new AtomicLong();
        private Date lastCheck;
        private boolean forceCheck;

        private AtomicReference<Thread> checkDestination = new AtomicReference<>();

        public AtomicReference<Thread> getCheckDestination() {
            return checkDestination;
        }

        public void setCheckDestination(AtomicReference<Thread> checkDestination) {
            this.checkDestination = checkDestination;
        }

        public AtomicLong getValue() {
            return value;
        }

        public void setValue(AtomicLong value) {
            this.value = value;
        }

        public Date getLastCheck() {
            return lastCheck;
        }

        public void setLastCheck(Date lastCheck) {
            this.lastCheck = lastCheck;
        }

        public boolean isForceCheck() {
            return forceCheck;
        }

        public void setForceCheck(boolean forceCheck) {
            this.forceCheck = forceCheck;
        }

        public FailureCounter() {
            this.lastCheck = new Date();
        }

        public long increment() {
            this.forceCheck = false;
            this.lastCheck = new Date();
            return value.incrementAndGet();
        }

        public long reset() {
            long oldVal = this.value.get();
            value.compareAndSet(oldVal, 0);
            lastCheck = new Date();
            forceCheck = false;
            return oldVal;
        }

    }
}
