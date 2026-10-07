package net.ravendb.client.documents.operations.etl.snowflake;

import net.ravendb.client.documents.operations.etl.EtlConfiguration;
import net.ravendb.client.documents.operations.etl.EtlType;

import java.util.ArrayList;
import java.util.List;

public class SnowflakeEtlConfiguration extends EtlConfiguration<SnowflakeConnectionString> {
    private Integer commandTimeout;
    private List<SnowflakeEtlTable> snowflakeTables = new ArrayList<>();

    public Integer getCommandTimeout() {
        return commandTimeout;
    }

    public void setCommandTimeout(Integer commandTimeout) {
        this.commandTimeout = commandTimeout;
    }

    public List<SnowflakeEtlTable> getSnowflakeTables() {
        return snowflakeTables;
    }

    public void setSnowflakeTables(List<SnowflakeEtlTable> snowflakeTables) {
        this.snowflakeTables = snowflakeTables;
    }

    @Override
    public EtlType getEtlType() {
        return EtlType.SNOWFLAKE;
    }
}
