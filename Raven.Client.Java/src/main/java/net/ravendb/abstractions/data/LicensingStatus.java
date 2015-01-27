package net.ravendb.abstractions.data;

import java.util.Map;

public class LicensingStatus {
  private Map<String, String> attributes;
  private String message;
  private String details;
  private String status;
  private boolean error;

  public Map<String, String> getAttributes() {
    return attributes;
  }

  public void setAttributes(Map<String, String> attributes) {
    this.attributes = attributes;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  public String getDetails() {
    return details;
  }

  public void setDetails(String details) {
    this.details = details;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public boolean isError() {
    return error;
  }

  public void setError(boolean error) {
    this.error = error;
  }

  public boolean isCommercial() {
    return status.startsWith("Commercial");
  }
}
