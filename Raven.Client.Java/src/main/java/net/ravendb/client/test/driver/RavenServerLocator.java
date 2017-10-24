package net.ravendb.client.test.driver;

public abstract class RavenServerLocator {

    public abstract String getServerPath();

    public String getCommand() {
        return getServerPath();
    }

    public String getCommandArguments() {
        return "";
    }

}
