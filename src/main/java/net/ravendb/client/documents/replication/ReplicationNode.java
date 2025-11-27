package net.ravendb.client.documents.replication;

import net.ravendb.client.primitives.UseSharpEnum;

/**
 * Data class for replication destination
 */
public abstract class ReplicationNode {
    /**
     * The name of the connection string specified in the
     * server configuration file.
     * Override all other properties of the destination
     */
    private String url;
    /**
     * The database to use
     */
    private String database;
    /**
     * Used to indicate whether external replication is disabled.
     */
    private boolean disabled;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getDatabase() {
        return database;
    }

    public void setDatabase(String database) {
        this.database = database;
    }

    public boolean isDisabled() {
        return disabled;
    }

    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
    }

    public boolean isEqualTo(ReplicationNode other) {
        return this.database.equalsIgnoreCase(other.getDatabase());
    }

    protected static long calculateStringHash(String s) {
        return (s == null || s.isEmpty()) ? 0 : s.hashCode();
    }

    /**
     * Replication type
     */
    public abstract ReplicationType getReplicationType();

    @UseSharpEnum
    public enum ReplicationType {
        EXTERNAL,
        PULL_AS_SINK,
        PULL_AS_HUB,
        INTERNAL,
        MIGRATION
    }
}
