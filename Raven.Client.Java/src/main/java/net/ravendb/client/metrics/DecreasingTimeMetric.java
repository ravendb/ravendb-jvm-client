package net.ravendb.client.metrics;

import net.ravendb.client.ConventionBase;
import org.apache.commons.lang.NotImplementedException;

public class DecreasingTimeMetric implements IRequestTimeMetric {

    private final IRequestTimeMetric requestTimeMetric;
    private final static double MAX_DECREASING_RATIO = 0.75;
    private final static double MIN_DECREASING_RATIO = 0.25;

    public DecreasingTimeMetric(IRequestTimeMetric requestTimeMetric) {
        this.requestTimeMetric = requestTimeMetric;
    }

    @Override
    public void update(long requestTimeInMilliseconds) {
        double rate = requestTimeMetric.rate();
        double maxRate = MAX_DECREASING_RATIO * rate;
        double minRate = MIN_DECREASING_RATIO * rate;

        double decreasingRate = rate - requestTimeInMilliseconds;

        if (decreasingRate > maxRate) {
            decreasingRate = maxRate;
        }

        if (decreasingRate < minRate) {
            decreasingRate = minRate;
        }

        requestTimeMetric.update((long) decreasingRate);
    }

    @Override
    public boolean rateSurpassed(ConventionBase conventions) {
        throw new NotImplementedException();
    }

    @Override
    public double rate() {
        throw new NotImplementedException();
    }

}
