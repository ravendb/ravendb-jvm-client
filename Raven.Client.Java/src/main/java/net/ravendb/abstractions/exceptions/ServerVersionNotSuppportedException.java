package net.ravendb.abstractions.exceptions;

/**
 * This exception is raised when server is not supported version.
 */
public class ServerVersionNotSuppportedException extends RuntimeException {

  public ServerVersionNotSuppportedException() {
    super();
  }

  public ServerVersionNotSuppportedException(String message, Throwable cause) {
    super(message, cause);
  }

  public ServerVersionNotSuppportedException(String message) {
    super(message);
  }

  public ServerVersionNotSuppportedException(Throwable cause) {
    super(cause);
  }

}
