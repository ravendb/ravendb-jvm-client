package net.ravendb.client.changes;


public interface IChangesConnectionState {
  void inc();

  void dec();

  void error(Exception e);
}
