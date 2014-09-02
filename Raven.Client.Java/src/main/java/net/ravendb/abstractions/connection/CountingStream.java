package net.ravendb.abstractions.connection;

import java.io.IOException;
import java.io.InputStream;


public class CountingStream extends InputStream {

  private InputStream inner;
  private long numberOfReadBytes = 0;

  public CountingStream(InputStream inner) {
    super();
    this.inner = inner;
  }

  public long getNumberOfReadBytes() {
    return numberOfReadBytes;
  }

  @Override
  public int read() throws IOException {
    if (inner == null) {
      return -1;
    }
    int value = inner.read();
    if (value >= 0) {
      numberOfReadBytes++;
    }
    return value;
  }

}
