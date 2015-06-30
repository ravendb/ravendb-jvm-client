package net.ravendb.abstractions.data;

import net.ravendb.abstractions.basic.SerializeUsingValue;
import net.ravendb.abstractions.basic.UseSharpEnum;

@UseSharpEnum
@SerializeUsingValue
public enum DataSubscriptionChangeTypes {

    NONE(0),

    SUBSCRIPTION_OPENED(1),

    SUBSCRIPTION_RELEASED(2);

    private int value;

    public int getValue() {
        return value;
    }

    private DataSubscriptionChangeTypes(int value) {
        this.value = value;
    }
}
