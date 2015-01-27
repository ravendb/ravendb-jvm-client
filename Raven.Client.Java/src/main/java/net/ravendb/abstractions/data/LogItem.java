package net.ravendb.abstractions.data;

import java.util.Date;


public class LogItem {
  private String level;
  private String message;
  private String loggerName;
  private String exception;
  private Date timeStamp;

  public String getLevel() {
    return level;
  }

  public void setLevel(String level) {
    this.level = level;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  public String getLoggerName() {
    return loggerName;
  }

  public void setLoggerName(String loggerName) {
    this.loggerName = loggerName;
  }

  public String getException() {
    return exception;
  }

  public void setException(String exception) {
    this.exception = exception;
  }

  public Date getTimeStamp() {
    return timeStamp;
  }

  public void setTimeStamp(Date timeStamp) {
    this.timeStamp = timeStamp;
  }

}
