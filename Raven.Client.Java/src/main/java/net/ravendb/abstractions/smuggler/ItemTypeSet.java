package net.ravendb.abstractions.smuggler;

import net.ravendb.abstractions.data.EnumSet;
import net.ravendb.client.SearchOptions;
import org.codehaus.jackson.annotate.JsonCreator;

import java.util.Arrays;

public class ItemTypeSet extends EnumSet<ItemType, ItemTypeSet> {

  public ItemTypeSet() {
    super(ItemType.class);
  }

  public ItemTypeSet(ItemType...values) {
    super(ItemType.class, Arrays.asList(values));
  }

  public static ItemTypeSet of(ItemType... values) {
    return new ItemTypeSet(values);
  }

  @JsonCreator
  static ItemTypeSet construct(int value) {
    return construct(new ItemTypeSet(), value);
  }

  @JsonCreator
  static ItemTypeSet construct(String value) {
    return construct(new ItemTypeSet(), value);
  }

}
