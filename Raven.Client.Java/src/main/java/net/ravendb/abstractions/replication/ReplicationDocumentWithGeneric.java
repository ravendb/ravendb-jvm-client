package net.ravendb.abstractions.replication;

import net.ravendb.abstractions.data.Constants;

import java.util.ArrayList;
import java.util.List;


public class ReplicationDocumentWithGeneric<TClass extends ReplicationDestination> {

  private List<TClass> destinations;
  private String id;
  private String source;
  private ReplicationClientConfiguration clientConfiguration;

  public ReplicationDocumentWithGeneric() {
    id = Constants.RAVEN_REPLICATION_DESTINATIONS;
    destinations = new ArrayList<>();
  }

  public ReplicationClientConfiguration getClientConfiguration() {
    return clientConfiguration;
  }

  public void setClientConfiguration(ReplicationClientConfiguration clientConfiguration) {
    this.clientConfiguration = clientConfiguration;
  }

  public List<TClass> getDestinations() {
    return destinations;
  }

  public String getId() {
    return id;
  }

  public String getSource() {
    return source;
  }

  public void setDestinations(List<TClass> destinations) {
    this.destinations = destinations;
  }

  public void setId(String id) {
    this.id = id;
  }

  public void setSource(String source) {
    this.source = source;
  }


}
