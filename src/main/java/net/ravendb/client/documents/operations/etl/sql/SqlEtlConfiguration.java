package net.ravendb.client.documents.operations.etl.sql;

import net.ravendb.client.documents.operations.etl.EtlConfiguration;
import net.ravendb.client.documents.operations.etl.EtlType;

import java.util.ArrayList;
import java.util.List;

public class SqlEtlConfiguration extends EtlConfiguration<SqlConnectionString> {
    private boolean parameterizeDeletes = true;
    private boolean forceQueryRecompile;
    private boolean quoteTables = true;
    private Integer commandTimeout;
    private List<SqlEtlTable> sqlTables;

    public SqlEtlConfiguration() {
        sqlTables = new ArrayList<>();
    }

    public boolean isParameterizeDeletes() {
        return parameterizeDeletes;
    }

    public void setParameterizeDeletes(boolean parameterizeDeletes) {
        this.parameterizeDeletes = parameterizeDeletes;
    }

    public boolean isForceQueryRecompile() {
        return forceQueryRecompile;
    }

    public void setForceQueryRecompile(boolean forceQueryRecompile) {
        this.forceQueryRecompile = forceQueryRecompile;
    }

    public boolean isQuoteTables() {
        return quoteTables;
    }

    public void setQuoteTables(boolean quoteTables) {
        this.quoteTables = quoteTables;
    }

    public Integer getCommandTimeout() {
        return commandTimeout;
    }

    public void setCommandTimeout(Integer commandTimeout) {
        this.commandTimeout = commandTimeout;
    }

    public List<SqlEtlTable> getSqlTables() {
        return sqlTables;
    }

    public void setSqlTables(List<SqlEtlTable> sqlTables) {
        this.sqlTables = sqlTables;
    }

    @Override
    public EtlType getEtlType() { return EtlType.SQL; }
}
