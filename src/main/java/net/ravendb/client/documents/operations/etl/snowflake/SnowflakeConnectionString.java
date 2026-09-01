package net.ravendb.client.documents.operations.etl.snowflake;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import net.ravendb.client.documents.operations.connectionStrings.ConnectionString;
import net.ravendb.client.serverwide.ConnectionStringType;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SnowflakeConnectionString extends ConnectionString {
    private String connectionString;

    public String getConnectionString() {
        return connectionString;
    }

    public void setConnectionString(String connectionString) {
        this.connectionString = connectionString;
    }

    @Override
    public ConnectionStringType getType() {
        return ConnectionStringType.SNOWFLAKE;
    }
}
