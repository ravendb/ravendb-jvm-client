package net.ravendb.abstractions.data;

public class UserPermission {
    private String user;
    private DatabaseInfo database;
    private String method;
    private boolean granted;
    private String reason;

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public DatabaseInfo getDatabase() {
        return database;
    }

    public void setDatabase(DatabaseInfo database) {
        this.database = database;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public boolean isGranted() {
        return granted;
    }

    public void setGranted(boolean granted) {
        this.granted = granted;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
