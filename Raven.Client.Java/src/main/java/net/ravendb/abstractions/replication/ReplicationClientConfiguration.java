package net.ravendb.abstractions.replication;

import net.ravendb.client.document.FailoverBehavior;


public class ReplicationClientConfiguration {
  private FailoverBehavior failoverBehavior;


  public FailoverBehavior getFailoverBehavior() {
    return failoverBehavior;
  }


  public void setFailoverBehavior(FailoverBehavior failoverBehavior) {
    this.failoverBehavior = failoverBehavior;
  }

}
