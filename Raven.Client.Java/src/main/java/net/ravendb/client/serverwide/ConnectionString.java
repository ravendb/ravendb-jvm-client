package net.ravendb.client.serverwide;

public abstract class ConnectionString {
    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public abstract ConnectionStringType getType();
}
