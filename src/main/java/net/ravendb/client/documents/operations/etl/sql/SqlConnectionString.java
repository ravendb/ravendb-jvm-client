package net.ravendb.client.documents.operations.etl.sql;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import net.ravendb.client.documents.operations.connectionStrings.ConnectionString;
import net.ravendb.client.serverwide.ConnectionStringType;
import org.apache.commons.lang3.NotImplementedException;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SqlConnectionString extends ConnectionString {
    private String connectionString;
    private String factoryName;

    @Override
    protected void validateImpl(List<String> errors) {

        try {
            SqlProviderParser.getSupportedProvider(factoryName);
        } catch (NotImplementedException e) {
            errors.add("Factory '" + factoryName + "' is not implemented yet.");
        } catch (Exception e) {
            errors.add("Unsupported factory '" + factoryName + "'");
        }

        if (connectionString == null || connectionString.isEmpty()) {
            errors.add("ConnectionString cannot be empty");
        }
    }

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

final class SqlProviderParser {

    private SqlProviderParser() {
    }

    public static SqlProvider getSupportedProvider(String factoryName) {

        switch (factoryName) {

            case "System.Data.SqlClient":
            case "Microsoft.Data.SqlClient":
                return SqlProvider.SQL_CLIENT;

            case "Npgsql":
                return SqlProvider.NPGSQL;

            case "System.Data.SqlServerCe.4.0":
                throw new UnsupportedOperationException(
                        "Factory '" + factoryName + "' is not implemented yet");

            case "System.Data.OleDb":
                throw new UnsupportedOperationException(
                        "Factory '" + factoryName + "' is not implemented yet");

            case "Oracle.ManagedDataAccess.Client":
                return SqlProvider.ORACLE_CLIENT;

            case "MySql.Data.MySqlClient":
            case "MySqlConnector.MySqlConnectorFactory":
                return SqlProvider.MYSQL_CONNECTOR_FACTORY;

            case "System.Data.SqlServerCe.3.5":
                throw new UnsupportedOperationException(
                        "Factory '" + factoryName + "' is not implemented yet");

            default:
                throw new UnsupportedOperationException(
                        "Factory '" + factoryName + "' is not supported");
        }
    }
}
