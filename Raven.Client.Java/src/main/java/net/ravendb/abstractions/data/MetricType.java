package net.ravendb.abstractions.data;

import net.ravendb.abstractions.basic.SerializeUsingValue;
import net.ravendb.abstractions.basic.UseSharpEnum;

@UseSharpEnum
@SerializeUsingValue
public enum MetricType {
  METER(1),
  HISTOGRAM(2);

  private MetricType(int value) {
    this.value = value;
  }

  public int getValue() {
    return value;
  }

  private int value;
}
