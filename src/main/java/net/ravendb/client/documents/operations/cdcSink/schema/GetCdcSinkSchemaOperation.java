package net.ravendb.client.documents.operations.cdcSink.schema;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.node.ObjectNode;
import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.operations.IMaintenanceOperation;
import net.ravendb.client.documents.operations.etl.sql.SqlConnectionString;
import net.ravendb.client.http.RavenCommand;
import net.ravendb.client.http.ServerNode;
import net.ravendb.client.json.ContentProviderHttpEntity;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.core5.http.ContentType;

import java.io.IOException;

/**
 * Browses the source database's tables, columns, PKs, and FKs annotated with CDC-specific hints
 * (suggested column type, capturability, table CDC enrollment) so callers can build a CDC mapping
 * without a manual schema dump. Calls {@code POST /admin/cdc-sink/schema}. Requires {@code DatabaseAdmin}.
 */
public class GetCdcSinkSchemaOperation implements IMaintenanceOperation<CdcSinkSourceSchema> {

    private final CdcSinkSchemaRequest _request;

    /**
     * Inline-credentials flavour. Use this when the connection hasn't been saved to
     * {@code databaseRecord.SqlConnectionStrings} yet.
     * @param connection the SQL connection string pointing at the CDC source database
     */
    public GetCdcSinkSchemaOperation(SqlConnectionString connection) {
        this(connection, null);
    }

    /**
     * Inline-credentials flavour. Use this when the connection hasn't been saved to
     * {@code databaseRecord.SqlConnectionStrings} yet.
     * @param connection the SQL connection string pointing at the CDC source database
     * @param schemas the provider-specific schema filter, or null for the provider default
     */
    public GetCdcSinkSchemaOperation(SqlConnectionString connection, String[] schemas) {
        this(forConnection(connection, schemas));
    }

    /**
     * Named-lookup flavour. The server resolves the connection string name against the database record.
     * @param connectionStringName the name of the SQL connection string in the database record
     */
    public GetCdcSinkSchemaOperation(String connectionStringName) {
        this(connectionStringName, null);
    }

    /**
     * Named-lookup flavour. The server resolves the connection string name against the database record.
     * @param connectionStringName the name of the SQL connection string in the database record
     * @param schemas the provider-specific schema filter, or null for the provider default
     */
    public GetCdcSinkSchemaOperation(String connectionStringName, String[] schemas) {
        this(forConnectionStringName(connectionStringName, schemas));
    }

    public GetCdcSinkSchemaOperation(CdcSinkSchemaRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("request cannot be null");
        }

        _request = request;
    }

    private static CdcSinkSchemaRequest forConnection(SqlConnectionString connection, String[] schemas) {
        if (connection == null) {
            throw new IllegalArgumentException("connection cannot be null");
        }

        CdcSinkSchemaRequest request = new CdcSinkSchemaRequest();
        request.setConnection(connection);
        request.setSchemas(schemas);
        return request;
    }

    private static CdcSinkSchemaRequest forConnectionStringName(String connectionStringName, String[] schemas) {
        if (connectionStringName == null) {
            throw new IllegalArgumentException("connectionStringName cannot be null");
        }

        CdcSinkSchemaRequest request = new CdcSinkSchemaRequest();
        request.setConnectionStringName(connectionStringName);
        request.setSchemas(schemas);
        return request;
    }

    @Override
    public RavenCommand<CdcSinkSourceSchema> getCommand(DocumentConventions conventions) {
        return new GetCdcSinkSchemaCommand(conventions, _request);
    }

    private static class GetCdcSinkSchemaCommand extends RavenCommand<CdcSinkSourceSchema> {
        private final CdcSinkSchemaRequest _request;
        private final DocumentConventions _conventions;

        public GetCdcSinkSchemaCommand(DocumentConventions conventions, CdcSinkSchemaRequest request) {
            super(CdcSinkSourceSchema.class);

            if (conventions == null) {
                throw new IllegalArgumentException("conventions cannot be null");
            }

            _conventions = conventions;
            _request = request;
        }

        // POST verb but no server-side state change - allow FastestNode failover.
        @Override
        public boolean isReadRequest() {
            return true;
        }

        @Override
        public HttpUriRequestBase createRequest(ServerNode node) {
            String url = node.getUrl() + "/databases/" + node.getDatabase() + "/admin/cdc-sink/schema";

            HttpPost request = new HttpPost(url);

            request.setEntity(new ContentProviderHttpEntity(outputStream -> {
                try (JsonGenerator generator = createSafeJsonGenerator(outputStream)) {
                    ObjectNode body = mapper.valueToTree(_request);
                    generator.writeTree(body);
                }
            }, ContentType.APPLICATION_JSON, _conventions));

            return request;
        }

        @Override
        public void setResponse(String response, boolean fromCache) throws IOException {
            if (response == null) {
                throwInvalidResponse();
            }

            result = mapper.readValue(response, resultClass);
        }
    }
}
