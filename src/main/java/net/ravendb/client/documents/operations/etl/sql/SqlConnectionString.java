package net.ravendb.client.documents.operations.etl.sql;

import net.ravendb.client.documents.operations.connectionStrings.ConnectionString;
import net.ravendb.client.serverwide.ConnectionStringType;

public class SqlConnectionString extends ConnectionString {
    private String connectionString;
    private String factoryName;

    public String getConnectionString() {
        return connectionString;
    }

    public void setConnectionString(String connectionString) {
        this.connectionString = connectionString;
    }

    public String getFactoryName() {
        return factoryName;
    }

    public void setFactoryName(String factoryName) {
        this.factoryName = factoryName;
    }

    @Override
    public ConnectionStringType getType() {
        return ConnectionStringType.SQL;
    }
}
