package net.ravendb.client.primitives;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class TimerService {
    public static final ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor();
}
