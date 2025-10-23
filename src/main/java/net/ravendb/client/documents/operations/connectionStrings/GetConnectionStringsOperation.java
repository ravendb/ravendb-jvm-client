package net.ravendb.client.documents.operations.connectionStrings;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.operations.AI.ConnectionStrings.AiConnectionString;
import net.ravendb.client.documents.operations.IMaintenanceOperation;
import net.ravendb.client.documents.operations.etl.RavenConnectionString;
import net.ravendb.client.documents.operations.etl.elasticSearch.ElasticSearchConnectionString;
import net.ravendb.client.documents.operations.etl.olap.OlapConnectionString;
import net.ravendb.client.documents.operations.etl.queue.QueueConnectionString;
import net.ravendb.client.documents.operations.etl.sql.SqlConnectionString;
import net.ravendb.client.http.RavenCommand;
import net.ravendb.client.http.ServerNode;
import net.ravendb.client.primitives.SharpEnum;
import net.ravendb.client.serverwide.ConnectionStringType;
import net.ravendb.client.util.UrlUtils;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class GetConnectionStringsOperation implements IMaintenanceOperation<GetConnectionStringsResult> {
    private final String _connectionStringName;
    private final ConnectionStringType _type;

    public GetConnectionStringsOperation(String connectionStringName, ConnectionStringType type) {
        _connectionStringName = connectionStringName;
        _type = type;
    }

    public GetConnectionStringsOperation() {
        _connectionStringName = null;
        _type = null;
    }

    @Override
    public RavenCommand<GetConnectionStringsResult> getCommand(DocumentConventions conventions) {
        return new GetConnectionStringCommand(_connectionStringName, _type);
    }

    private static class GetConnectionStringCommand extends RavenCommand<GetConnectionStringsResult> {
        private final String _connectionStringName;
        private final ConnectionStringType _type;

        public GetConnectionStringCommand(String connectionStringName, ConnectionStringType type) {
            super(GetConnectionStringsResult.class);
            _connectionStringName = connectionStringName;
            _type = type;
        }

        @Override
        public boolean isReadRequest() {
            return true;
        }

        @Override
        public HttpUriRequestBase createRequest(ServerNode node) {
            String url = node.getUrl() + "/databases/" + node.getDatabase() + "/admin/connection-strings";
            if (_connectionStringName != null) {
                url += "?connectionStringName=" + UrlUtils.escapeDataString(_connectionStringName) + "&type=" + SharpEnum.value(_type);
            }

            return new HttpGet(url);
        }

        @Override
        public void setResponse(String response, boolean fromCache) throws IOException {
            if (response == null) {
                throwInvalidResponse();
            }

            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true);
            Map<String, Object> raw = mapper.readValue(response, new TypeReference<Map<String, Object>>() {});
            GetConnectionStringsResult result = mapper.readValue(response, GetConnectionStringsResult.class);

            if (result.getRavenConnectionStrings() != null) {
                Map<String, RavenConnectionString> typed = new HashMap<>();
                for (Map.Entry<String, RavenConnectionString> entry : result.getRavenConnectionStrings().entrySet()) {
                    RavenConnectionString value = mapper.convertValue(entry.getValue(), RavenConnectionString.class);
                    typed.put(entry.getKey(), value);
                }
                result.setRavenConnectionStrings(typed);
            }

            if (result.getSqlConnectionStrings() != null) {
                Map<String, SqlConnectionString> typed = new HashMap<>();
                for (Map.Entry<String, SqlConnectionString> entry : result.getSqlConnectionStrings().entrySet()) {
                    SqlConnectionString value = mapper.convertValue(entry.getValue(), SqlConnectionString.class);
                    typed.put(entry.getKey(), value);
                }
                result.setSqlConnectionStrings(typed);
            }

            if (result.getElasticSearchConnectionStrings() != null) {
                Map<String, ElasticSearchConnectionString> typed = new HashMap<>();
                for (Map.Entry<String, ElasticSearchConnectionString> entry : result.getElasticSearchConnectionStrings().entrySet()) {
                    ElasticSearchConnectionString value = mapper.convertValue(entry.getValue(), ElasticSearchConnectionString.class);
                    typed.put(entry.getKey(), value);
                }
                result.setElasticSearchConnectionStrings(typed);
            }

            if (result.getQueueConnectionStrings() != null) {
                Map<String, QueueConnectionString> typed = new HashMap<>();
                for (Map.Entry<String, QueueConnectionString> entry : result.getQueueConnectionStrings().entrySet()) {
                    QueueConnectionString value = mapper.convertValue(entry.getValue(), QueueConnectionString.class);
                    typed.put(entry.getKey(), value);
                }
                result.setQueueConnectionStrings(typed);
            }

            if (result.getOlapConnectionStrings() != null) {
                Map<String, OlapConnectionString> typed = new HashMap<>();
                for (Map.Entry<String, OlapConnectionString> entry : result.getOlapConnectionStrings().entrySet()) {
                    OlapConnectionString value = mapper.convertValue(entry.getValue(), OlapConnectionString.class);
                    typed.put(entry.getKey(), value);
                }
                result.setOlapConnectionStrings(typed);
            }

            if (result.getAiConnectionStrings() != null) {
                Map<String, AiConnectionString> typed = new HashMap<>();
                for (Map.Entry<String, AiConnectionString> entry : result.getAiConnectionStrings().entrySet()) {
                    AiConnectionString value = mapper.convertValue(entry.getValue(), AiConnectionString.class);
                    typed.put(entry.getKey(), value);
                }
                result.setAiConnectionStrings(typed);
            }

            this.result = result;
        }
    }
}
