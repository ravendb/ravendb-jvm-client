package net.ravendb.client.utils;

import net.ravendb.abstractions.basic.CleanCloseable;

public class Closer {
  public static void close(CleanCloseable objectToClose) {
    if (objectToClose != null) {
      objectToClose.close();
    }
  }
}
