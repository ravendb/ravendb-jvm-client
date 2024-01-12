package net.ravendb.client.serverwide.operations.builder;

import net.ravendb.client.serverwide.operations.integrations.postgreSql.PostgreSqlConfiguration;

public interface IIntegrationConfigurationBuilder {
    IIntegrationConfigurationBuilder configurePostgreSql(PostgreSqlConfiguration configuration);
}
