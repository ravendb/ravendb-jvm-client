package net.ravendb.abstractions.exceptions;


public class AggregateException extends RuntimeException {
  private Exception[] inner;

  public AggregateException(Exception[] inner) {
    this.inner = inner;
  }

  public AggregateException(String message, Exception[] inner) {
    super(message);
    this.inner = inner;
  }


  public Exception[] getInner() {
    return inner;
  }


}
