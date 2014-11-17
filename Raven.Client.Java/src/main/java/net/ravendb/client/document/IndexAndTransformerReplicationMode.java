package net.ravendb.client.document;

import net.ravendb.abstractions.basic.SerializeUsingValue;
import net.ravendb.abstractions.basic.UseSharpEnum;

@UseSharpEnum
@SerializeUsingValue
public enum IndexAndTransformerReplicationMode {

  /**
   *  No indexes or transformers are updated to replicated instances.
   */
  NONE(0),

  /**
   * All indexes are replicated.
   */
  INDEXES(2),

  /**
   * All transformers are replicated.
   */
  TRANSFORMERS(4);

  private int value;

  /**
   * @return the value
   */
  public int getValue() {
    return value;
  }

  private IndexAndTransformerReplicationMode(int value) {
    this.value = value;
  }
}
