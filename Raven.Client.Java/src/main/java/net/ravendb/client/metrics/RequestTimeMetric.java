package net.ravendb.client.metrics;

import net.ravendb.client.ConventionBase;

import java.util.concurrent.TimeUnit;

public class RequestTimeMetric implements IRequestTimeMetric  {

    private final EWMA ewma;

    private final static double SWTICH_BACK_RATIO = 0.75;

    private volatile boolean surpassed;

    public RequestTimeMetric() {
        ewma = new EWMA(EWMA.M1_ALPHA, 1, TimeUnit.MILLISECONDS);

        for (int i = 0; i < 60; i++) {
            update(0L);
        }
    }

    @Override
    public void update(long requestTimeInMilliseconds) {
        ewma.update(requestTimeInMilliseconds);
        ewma.tick();
    }

    @Override
    public boolean rateSurpassed(ConventionBase conventions) {
        double requestTimeThresholdInMiliseconds = conventions.getRequestTimeThresholdInMiliseconds();
        double rate = rate();

        if (surpassed) {
            return surpassed = rate >= SWTICH_BACK_RATIO * requestTimeThresholdInMiliseconds;
        }
        return surpassed = rate >= requestTimeThresholdInMiliseconds;
    }

    @Override
    public double rate() {
        return ewma.rate(TimeUnit.MILLISECONDS);
    }
}
