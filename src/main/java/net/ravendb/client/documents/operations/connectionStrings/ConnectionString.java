package net.ravendb.client.documents.operations.connectionStrings;

import net.ravendb.client.serverwide.ConnectionStringType;

public abstract class ConnectionString {
    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @SuppressWarnings("SameReturnValue")
    public abstract ConnectionStringType getType();
}
