package net.ravendb.client.serverwide.operations.integrations.postgreSql;

public class PostgreSqlConfiguration {

    private PostgreSqlAuthenticationConfiguration authentication;

    public PostgreSqlAuthenticationConfiguration getAuthentication() {
        return authentication;
    }

    public void setAuthentication(PostgreSqlAuthenticationConfiguration authentication) {
        this.authentication = authentication;
    }
}
