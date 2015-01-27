package net.ravendb.abstractions.basic;

import java.io.Closeable;


public interface CleanCloseable extends Closeable {
  @Override
  public void close();
}
