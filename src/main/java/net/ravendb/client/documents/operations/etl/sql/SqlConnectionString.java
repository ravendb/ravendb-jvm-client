package net.ravendb.client.documents.operations.etl.sql;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import net.ravendb.client.documents.operations.connectionStrings.ConnectionString;
import net.ravendb.client.serverwide.ConnectionStringType;

@JsonIgnoreProperties(ignoreUnknown = true)
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
