package net.ravendb.abstractions.smuggler;

import net.ravendb.abstractions.basic.SerializeUsingValue;
import net.ravendb.abstractions.basic.UseSharpEnum;

@UseSharpEnum
@SerializeUsingValue
public enum ItemType {

  DOCUMENTS(1),
  INDEXES(2),
  ATTACHMENTS(4),
  TRANSFORMERS(8),
  REMOVE_ANALYZERS (0x8000);

  ItemType(int value) {
    this.value = value;
  }

  private int value;

  public int getValue() {
    return value;
  }
}

