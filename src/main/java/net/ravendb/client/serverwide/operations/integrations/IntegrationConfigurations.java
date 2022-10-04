package net.ravendb.client.serverwide.operations.integrations;

import net.ravendb.client.serverwide.operations.integrations.postgreSql.PostgreSqlConfiguration;

public class IntegrationConfigurations {
    private PostgreSqlConfiguration postgreSql;

    public PostgreSqlConfiguration getPostgreSql() {
        return postgreSql;
    }

    public void setPostgreSql(PostgreSqlConfiguration postgreSql) {
        this.postgreSql = postgreSql;
    }
}
