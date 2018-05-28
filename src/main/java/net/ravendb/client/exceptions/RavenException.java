package net.ravendb.client.exceptions;

public class RavenException extends RuntimeException {

    private boolean reachedLeader;

    public RavenException() {
    }

    public RavenException(String message) {
        super(message);
    }

    public RavenException(String message, Throwable cause) {
        super(message, cause);
    }

    public boolean isReachedLeader() {
        return reachedLeader;
    }

    public void setReachedLeader(boolean reachedLeader) {
        this.reachedLeader = reachedLeader;
    }

    public static RavenException generic(String error, String json) {
        return new RavenException(error + ". Response: " + json);
    }
}
