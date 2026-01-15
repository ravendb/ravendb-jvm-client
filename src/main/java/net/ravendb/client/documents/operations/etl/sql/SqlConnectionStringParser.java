package net.ravendb.client.documents.operations.etl.sql;

import net.ravendb.client.util.TupleDatabaseServerInfo;

public final class SqlConnectionStringParser {

    private SqlConnectionStringParser() {
    }

    static TupleDatabaseServerInfo getDatabaseAndServerFromConnectionString(String factoryName, String connectionString) {
        String database;
        String server;

        switch (SqlProviderParser.getSupportedProvider(factoryName)) {

            case SQL_CLIENT:
                database = getConnectionStringValue(connectionString,
                        new String[]{"Initial Catalog", "Database"});

                if (database == null)
                    database = "master";

                server = getConnectionStringValue(connectionString,
                        new String[]{"Data Source", "Server", "Address", "Addr", "Network Address"});
                break;

            case NPGSQL:
                database = getConnectionStringValue(connectionString,
                        new String[]{"Database"});

                server = getConnectionStringValue(connectionString,
                        new String[]{"Host", "Data Source", "Server"});

                String postgrePort = getConnectionStringValue(connectionString,
                        new String[]{"Port"});

                if (postgrePort != null && !postgrePort.isEmpty())
                    server += ":" + postgrePort;

                break;

            case MY_SQL_CLIENT:
            case MYSQL_CONNECTOR_FACTORY:
                database = getConnectionStringValue(connectionString,
                        new String[]{"Database", "Initial Catalog"});

                if (database == null)
                    database = "mysql";

                server = getConnectionStringValue(connectionString,
                        new String[]{"Host", "Server", "Data Source", "DataSource",
                                "Address", "Addr", "Network Address"});

                if (server == null)
                    server = "localhost";

                String mysqlPort = getConnectionStringValue(connectionString,
                        new String[]{"Port"});

                if (mysqlPort != null && !mysqlPort.isEmpty())
                    server += ":" + mysqlPort;

                break;

            case ORACLE_CLIENT:
                server = null;
                database = null;

                String dataSource = getConnectionStringValue(connectionString,
                        new String[]{"Data Source"});

                if (dataSource != null && !dataSource.trim().isEmpty()) {

                    server = getOracleDataSourceSubValue(dataSource, "HOST");

                    if (server != null) {
                        String port = getOracleDataSourceSubValue(dataSource, "PORT");
                        if (port != null)
                            server += ":" + port;
                    }

                    database = getOracleDataSourceSubValue(dataSource, "SERVICE_NAME");
                    if (database == null)
                        database = getOracleDataSourceSubValue(dataSource, "SID");

                    if (server == null) {
                        String[] parts = dataSource.split("@", 2);

                        if (parts.length == 2) {
                            server = parts[1];
                        } else {
                            server = dataSource;
                        }
                    }
                }

                break;

            default:
                throw new UnsupportedOperationException(
                        "Factory '" + factoryName + "' is not supported");
        }

        return new TupleDatabaseServerInfo(database, server);
    }

    public static String getConnectionStringValue(String connectionString, String[] keyNames) {
        String[] parts = connectionString.split(";");

        for (String part : parts) {
            String[] keyValue = part.split("=", 2);

            if (keyValue.length != 2)
                continue;

            String key = keyValue[0].trim();
            String value = keyValue[1].trim();

            for (String expected : keyNames) {
                if (key.equalsIgnoreCase(expected))
                    return value;
            }
        }

        return null;
    }

    static String getOracleDataSourceSubValue(String dataSourceValue, String key) {
        int indexOf = dataSourceValue.toLowerCase().indexOf(key.toLowerCase());

        if (indexOf == -1)
            return null;

        String subString = dataSourceValue.substring(indexOf);

        int closingBracket = subString.indexOf(')');
        if (closingBracket == -1)
            return null;

        String subValue = subString.substring(0, closingBracket);

        return getConnectionStringValue(subValue, new String[]{key});
    }
}
