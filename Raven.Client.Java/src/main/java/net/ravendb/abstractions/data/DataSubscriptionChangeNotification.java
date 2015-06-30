package net.ravendb.abstractions.data;

import net.ravendb.abstractions.basic.EventArgs;

public class DataSubscriptionChangeNotification extends EventArgs {

    private long id;
    private DataSubscriptionChangeTypes type;

    /**
     *  Subscription identifier for which a notification was created
     */
    public long getId() {
        return id;
    }

    /**
     *  Subscription identifier for which a notification was created
     */
    public void setId(long id) {
        this.id = id;
    }

    /**
     * Type of subscription change
     */
    public DataSubscriptionChangeTypes getType() {
        return type;
    }

    /**
     * Type of subscription change
     */
    public void setType(DataSubscriptionChangeTypes type) {
        this.type = type;
    }
}
