package net.ravendb.client.documents.operations.connectionStrings;

import net.ravendb.client.documents.operations.etl.sql.SqlConnectionString;
import net.ravendb.client.documents.operations.etl.RavenConnectionString;

import java.util.Map;

public class GetConnectionStringsResult {
    private Map<String, RavenConnectionString> ravenConnectionStrings;
    private Map<String, SqlConnectionString> sqlConnectionStrings;

    public Map<String, RavenConnectionString> getRavenConnectionStrings() {
        return ravenConnectionStrings;
    }

    public void setRavenConnectionStrings(Map<String, RavenConnectionString> ravenConnectionStrings) {
        this.ravenConnectionStrings = ravenConnectionStrings;
    }

    public Map<String, SqlConnectionString> getSqlConnectionStrings() {
        return sqlConnectionStrings;
    }

    public void setSqlConnectionStrings(Map<String, SqlConnectionString> sqlConnectionStrings) {
        this.sqlConnectionStrings = sqlConnectionStrings;
    }
}
