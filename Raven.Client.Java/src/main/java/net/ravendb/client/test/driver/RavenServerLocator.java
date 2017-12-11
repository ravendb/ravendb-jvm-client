package net.ravendb.client.test.driver;

public abstract class RavenServerLocator {

    public abstract String getServerPath();

    public boolean withHttps() {
        return false;
    }

    public String getCommand() {
        return getServerPath();
    }

    public String[] getCommandArguments() {
        return new String[0];
    }

    public String getServerCertificatePath() {
        throw new UnsupportedOperationException();
    }

}
