package net.ravendb.client.primitives;

import java.util.Date;

public class CancellationTokenSource {

    boolean cancelled = false;

    protected Long cancelAfterDate;

    public CancellationToken getToken() {
        return new CancellationToken();
    }

    public class CancellationToken {
        protected CancellationToken() {

        }

        @SuppressWarnings("boxing")
        public boolean isCancellationRequested() {
            return cancelled || (cancelAfterDate != null && new Date().getTime() > cancelAfterDate);
        }

        public void throwIfCancellationRequested() {
            if (isCancellationRequested()) {
                throw new OperationCancelledException();
            }
        }
    }

    public void cancel() {
        cancelled = true;
    }

    @SuppressWarnings("boxing")
    public void cancelAfter(long timeoutInMillis) {
        this.cancelAfterDate = new Date().getTime() + timeoutInMillis;
    }
}
