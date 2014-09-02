package net.ravendb.abstractions.replication;

import java.util.ArrayList;
import java.util.List;

import net.ravendb.abstractions.data.Constants;


public class ReplicationDocument {

  private List<ReplicationDestination> destinations;
  private String id;
  private String source;
  private ReplicationClientConfiguration clientConfiguration;

  public ReplicationDocument() {
    id = Constants.RAVEN_REPLICATION_DESTINATIONS;
    destinations = new ArrayList<>();
  }

  public ReplicationClientConfiguration getClientConfiguration() {
    return clientConfiguration;
  }

  public void setClientConfiguration(ReplicationClientConfiguration clientConfiguration) {
    this.clientConfiguration = clientConfiguration;
  }

  public List<ReplicationDestination> getDestinations() {
    return destinations;
  }

  public String getId() {
    return id;
  }

  public String getSource() {
    return source;
  }

  public void setDestinations(List<ReplicationDestination> destinations) {
    this.destinations = destinations;
  }

  public void setId(String id) {
    this.id = id;
  }

  public void setSource(String source) {
    this.source = source;
  }


}
