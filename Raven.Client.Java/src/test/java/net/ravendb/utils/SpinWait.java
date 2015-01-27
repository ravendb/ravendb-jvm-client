package net.ravendb.utils;

import java.util.Date;

import net.ravendb.abstractions.closure.Function0;
import net.ravendb.abstractions.util.TimeUtils;


public class SpinWait {
  @SuppressWarnings("boxing")
  public static boolean spinUntil(Function0<Boolean> condition, Long timeout) {
    long startTime = new Date().getTime();
    while (!condition.apply()) {
      if (timeout == 0L) {
        return false;
      }
      TimeUtils.cleanSleep(1);

      if (timeout <= new Date().getTime() - startTime) {
        return false;
      }

    }
    return true;

  }
}
