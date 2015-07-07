package net.ravendb.client.metrics;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * An exponentially-weighted moving average
 * http://www.teamquest.com/pdfs/whitepaper/ldavg1.pdf
 * http://www.teamquest.com/pdfs/whitepaper/ldavg2.pdf
 */
public class EWMA {

    private static final double M1_SECOND = 1 - Math.exp(-1);
    public static final double M1_ALPHA = 1 - Math.exp(-5 / 60.0);
    private static final double M5_ALPHA = 1 - Math.exp(-5 / 60.0 / 5);
    private static final double M15_ALPHA = 1 - Math.exp(-5 / 60.0 / 15);

    private final AtomicLong _uncounted = new AtomicLong(0);
    private final double _alpha;
    private final double _interval;
    private volatile boolean _initialized;
    private volatile double _rate;

    /**
     * Creates a new EWMA which is equivalent to one second load average and which expects to be ticked every 1 seconds.
     */
    public static EWMA oneSecondEWMA() {
        return new EWMA(M1_SECOND, 1, TimeUnit.SECONDS);
    }

    /**
     * Creates a new EWMA which is equivalent to the UNIX one minute load average and which expects to be ticked every 5 seconds.
     * @return
     */
    public static EWMA oneMinuteEWMA() {
        return new EWMA(M1_ALPHA, 5, TimeUnit.SECONDS);
    }

    /**
     * Creates a new EWMA which is equivalent to the UNIX five minute load average and which expects to be ticked every 5 seconds.
     */
    public static EWMA fiveMinuteEWMA() {
        return new EWMA(M5_ALPHA, 5, TimeUnit.SECONDS);
    }

    /**
     * Creates a new EWMA which is equivalent to the UNIX fifteen minute load average and which expects to be ticked every 5 seconds.
     */
    public static EWMA fifteenMinuteEWMA() {
        return new EWMA(M15_ALPHA, 5, TimeUnit.SECONDS);
    }

    /**
     * Create a new EWMA with a specific smoothing constant.
     * @param alpha The smoothing constant
     * @param internal The expected tick interval
     * @param intervalUnit The time unit of the tick interval
     */
    public EWMA(double alpha, long internal, TimeUnit intervalUnit) {
        _interval = intervalUnit.toNanos(internal);
        _alpha = alpha;
    }

    /**
     * Update the moving average with a new value.
     */
    public void update(long n) {
        _uncounted.addAndGet(n);
    }

    /**
     * Mark the passage of time and decay the current rate accordingly.
     */
    public void tick() {
        long count = _uncounted.getAndSet(0);
        double instantRate = count / _interval;
        if (_initialized) {
            _rate += _alpha * (instantRate - _rate);
        } else {
            _rate = instantRate;
            _initialized = true;
        }
    }

    /**
     * Returns the rate in the give units of time.
     */
    public double rate(TimeUnit rateUnit) {
        long nanos = rateUnit.toNanos(1);
        return _rate * nanos;
    }
}
