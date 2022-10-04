package net.ravendb.client.documents.operations.connectionStrings;

import net.ravendb.client.documents.operations.etl.elasticSearch.ElasticSearchConnectionString;
import net.ravendb.client.documents.operations.etl.olap.OlapConnectionString;
import net.ravendb.client.documents.operations.etl.queue.QueueConnectionString;
import net.ravendb.client.documents.operations.etl.sql.SqlConnectionString;
import net.ravendb.client.documents.operations.etl.RavenConnectionString;

import java.util.Map;

public class GetConnectionStringsResult {
    private Map<String, RavenConnectionString> ravenConnectionStrings;
    private Map<String, SqlConnectionString> sqlConnectionStrings;
    private Map<String, OlapConnectionString> olapConnectionStrings;

    private Map<String, ElasticSearchConnectionString> elasticSearchConnectionStrings;
    private Map<String, QueueConnectionString> queueConnectionStrings;

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

    public Map<String, OlapConnectionString> getOlapConnectionStrings() {
        return olapConnectionStrings;
    }

    public void setOlapConnectionStrings(Map<String, OlapConnectionString> olapConnectionStrings) {
        this.olapConnectionStrings = olapConnectionStrings;
    }

    public Map<String, ElasticSearchConnectionString> getElasticSearchConnectionStrings() {
        return elasticSearchConnectionStrings;
    }

    public void setElasticSearchConnectionStrings(Map<String, ElasticSearchConnectionString> elasticSearchConnectionStrings) {
        this.elasticSearchConnectionStrings = elasticSearchConnectionStrings;
    }

    public Map<String, QueueConnectionString> getQueueConnectionStrings() {
        return queueConnectionStrings;
    }

    public void setQueueConnectionStrings(Map<String, QueueConnectionString> queueConnectionStrings) {
        this.queueConnectionStrings = queueConnectionStrings;
    }
}
