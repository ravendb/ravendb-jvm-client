package net.ravendb.abstractions.data;

import net.ravendb.abstractions.basic.SerializeUsingValue;
import net.ravendb.abstractions.basic.UseSharpEnum;

@UseSharpEnum
@SerializeUsingValue
public enum PrefetchingUser {
  INDEXER(1),

  REPLICATOR(2),

  SQL_REPLICATOR(3);


  private int value;

  private PrefetchingUser(int value) {
    this.value = value;
  }

  public int getValue() {
    return value;
  }
}
