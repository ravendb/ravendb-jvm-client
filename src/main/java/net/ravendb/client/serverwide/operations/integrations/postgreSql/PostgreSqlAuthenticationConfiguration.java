package net.ravendb.client.serverwide.operations.integrations.postgreSql;

import java.util.List;

public class PostgreSqlAuthenticationConfiguration {
    private List<PostgreSqlUser> users;

    public List<PostgreSqlUser> getUsers() {
        return users;
    }

    public void setUsers(List<PostgreSqlUser> users) {
        this.users = users;
    }
}
