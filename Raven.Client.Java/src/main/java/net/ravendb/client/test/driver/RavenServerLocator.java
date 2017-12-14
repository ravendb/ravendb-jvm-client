package net.ravendb.client.test.driver;

import org.apache.commons.lang3.StringUtils;

public abstract class RavenServerLocator {

    public static final String ENV_SERVER_PATH = "RAVENDB_JAVA_TEST_SERVER_PATH";

    public String getServerPath() {
        String path = System.getenv(ENV_SERVER_PATH);
        if (StringUtils.isBlank(path)) {
            throw new IllegalStateException("Unable to find RavenDB server path. " +
                    "Please make sure " + ENV_SERVER_PATH + " environment variable is set and is valid " +
                    "(current value = " + path + ")");
        }

        return path;
    }

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
