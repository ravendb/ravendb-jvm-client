package net.ravendb.abstractions.exceptions;

import net.ravendb.abstractions.data.Etag;

public class ConcurrencyException extends RuntimeException {

  private Etag expectedEtag;
  private Etag actualEtag;

  /**
   * Expected Etag.
   */
  public Etag getExpectedEtag() {
    return expectedEtag;
  }

  /**
   * Actual Etag.
   */
  public Etag getActualEtag() {
    return actualEtag;
  }

  public ConcurrencyException(Etag expectedEtag, Etag actualEtag, String error, Throwable cause) {
    super("Expected Etag: " + expectedEtag + ", actual: " + actualEtag + " " + error + ":" + cause.getMessage(), cause);
    this.expectedEtag = expectedEtag;
    this.actualEtag = actualEtag;
  }

}
