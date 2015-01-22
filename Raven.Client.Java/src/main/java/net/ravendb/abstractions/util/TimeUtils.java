package net.ravendb.abstractions.util;


public class TimeUtils {
  public static void cleanSleep(int duration) {
    try {
      Thread.sleep(duration);
    } catch (InterruptedException e) {
    }
  }
}
