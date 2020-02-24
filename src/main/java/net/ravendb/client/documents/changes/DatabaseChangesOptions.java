package net.ravendb.client.documents.changes;

import java.util.Objects;

public class DatabaseChangesOptions {
    private String databaseName;
    private String nodeTag;

    public String getDatabaseName() {
        return databaseName;
    }

    public void setDatabaseName(String databaseName) {
        this.databaseName = databaseName;
    }

    public String getNodeTag() {
        return nodeTag;
    }

    public void setNodeTag(String nodeTag) {
        this.nodeTag = nodeTag;
    }


    public DatabaseChangesOptions() {
    }

    public DatabaseChangesOptions(String databaseName, String nodeTag) {
        this.databaseName = databaseName;
        this.nodeTag = nodeTag;
    }

    private String toLower(String input) {
        if (input == null) {
            return null;
        }

        return input.toLowerCase();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DatabaseChangesOptions that = (DatabaseChangesOptions) o;
        return Objects.equals(toLower(databaseName), toLower(that.databaseName)) &&
                Objects.equals(nodeTag, that.nodeTag);
    }

    @Override
    public int hashCode() {
        return Objects.hash(toLower(databaseName), nodeTag);
    }
}
