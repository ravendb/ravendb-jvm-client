package net.ravendb.client.http;

import net.ravendb.client.primitives.UseSharpEnum;

public class ServerNode {

    @UseSharpEnum
    public enum Role {
        NONE,
        PROMOTABLE,
        MEMBER,
        REHAB
    }

    private String url;
    private String database;
    private String clusterTag;
    private Role serverRole;

    public ServerNode() {
    }

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

    public String getClusterTag() {
        return clusterTag;
    }

    public void setClusterTag(String clusterTag) {
        this.clusterTag = clusterTag;
    }

    public Role getServerRole() {
        return serverRole;
    }

    public void setServerRole(Role serverRole) {
        this.serverRole = serverRole;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ServerNode that = (ServerNode) o;

        if (url != null ? !url.equals(that.url) : that.url != null) return false;
        return database != null ? database.equals(that.database) : that.database == null;
    }

    @Override
    public int hashCode() {
        int result = url != null ? url.hashCode() : 0;
        result = 31 * result + (database != null ? database.hashCode() : 0);
        return result;
    }

}
