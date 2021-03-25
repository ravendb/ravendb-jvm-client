package net.ravendb.client;


import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

public class RavenTestHelper {

    public static Date utcToday() {
        Instant today = Instant.now()
                .truncatedTo(ChronoUnit.DAYS);

        return Date.from(today);
    }

}
