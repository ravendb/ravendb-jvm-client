package net.ravendb.abstractions.data;

import net.ravendb.abstractions.basic.EventArgs;


public class TransformerChangeNotification extends EventArgs {
  private TransformerChangeTypes type;
  private String name;

  /**
   * Type of change that occurred on transformer.
   */
  public TransformerChangeTypes getType() {
    return type;
  }

  /**
   * Type of change that occurred on transformer.
   * @param type
   */
  public void setType(TransformerChangeTypes type) {
    this.type = type;
  }

  /**
   * Name of transformer for which notification was created
   */
  public String getName() {
    return name;
  }

  /**
   * Name of transformer for which notification was created
   * @param name
   */
  public void setName(String name) {
    this.name = name;
  }

  @Override
  public String toString() {
    return String.format("%s on %s", type, name);
  }



}
