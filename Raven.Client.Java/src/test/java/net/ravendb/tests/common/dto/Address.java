package net.ravendb.tests.common.dto;


public class Address {
  private String id;
  private String street;
  private int zipCode;
  
  public String getId() {
    return id;
  }
  
  public void setId(String id) {
    this.id = id;
  }
  
  public String getStreet() {
    return street;
  }
  
  public void setStreet(String street) {
    this.street = street;
  }
  
  public int getZipCode() {
    return zipCode;
  }
  
  public void setZipCode(int zipCode) {
    this.zipCode = zipCode;
  }
}
