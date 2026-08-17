package net.ravendb.client.serverwide.operations.connectionStrings;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import net.ravendb.client.documents.operations.AI.AiConnectionString;
import net.ravendb.client.documents.operations.connectionStrings.ConnectionString;
import net.ravendb.client.documents.operations.etl.RavenConnectionString;
import net.ravendb.client.documents.operations.etl.elasticSearch.ElasticSearchConnectionString;
import net.ravendb.client.documents.operations.etl.olap.OlapConnectionString;
import net.ravendb.client.documents.operations.etl.queue.QueueConnectionString;
import net.ravendb.client.documents.operations.etl.sql.SqlConnectionString;
import net.ravendb.client.extensions.JsonExtensions;
import net.ravendb.client.primitives.SharpEnum;
import net.ravendb.client.serverwide.ConnectionStringType;

import java.io.IOException;
import java.util.Arrays;

/**
 * Represents a server-wide connection string that is automatically propagated to all databases in the
 * cluster (unless explicitly excluded). Wraps a standard {@link ConnectionString} with additional
 * server-wide configuration such as {@link #getExcludedDatabases()}.
 *
 * <p>
 * On the wire the underlying connection string is <em>inlined</em> alongside {@code Type} and
 * {@code ExcludedDatabases}, rather than nested under a property, which is why this class uses a custom
 * serializer and deserializer.
 * </p>
 */
@JsonSerialize(using = ServerWideConnectionString.ServerWideConnectionStringSerializer.class)
@JsonDeserialize(using = ServerWideConnectionString.ServerWideConnectionStringDeserializer.class)
public class ServerWideConnectionString {

    private ConnectionString connectionString;
    private String[] excludedDatabases;

    /**
     * @return the underlying connection string definition (e.g. {@link RavenConnectionString},
     *         {@link SqlConnectionString}, etc.)
     */
    public ConnectionString getConnectionString() {
        return connectionString;
    }

    public void setConnectionString(ConnectionString connectionString) {
        this.connectionString = connectionString;
    }

    /**
     * @return an optional list of database names that should not receive this server-wide connection string.
     *         When null or empty, the connection string is propagated to all databases.
     */
    public String[] getExcludedDatabases() {
        return excludedDatabases;
    }

    public void setExcludedDatabases(String[] excludedDatabases) {
        this.excludedDatabases = excludedDatabases;
    }

    /**
     * @return the name of the connection string, delegated from the underlying {@link #getConnectionString()}
     */
    @JsonIgnore
    public String getName() {
        return connectionString != null ? connectionString.getName() : null;
    }

    /**
     * @return the type of the connection string (Raven, Sql, Olap, etc.), delegated from the underlying
     *         {@link #getConnectionString()}
     */
    @JsonIgnore
    public ConnectionStringType getType() {
        return connectionString != null ? connectionString.getType() : ConnectionStringType.NONE;
    }

    /**
     * Determines whether the specified database is excluded from receiving this server-wide connection string.
     * @param databaseName the name of the database to check
     * @return true if the database is in the excluded list; otherwise false
     */
    public boolean isExcluded(String databaseName) {
        if (excludedDatabases == null) {
            return false;
        }

        return Arrays.stream(excludedDatabases).anyMatch(x -> x != null && x.equalsIgnoreCase(databaseName));
    }

    /**
     * Flattens this server-wide connection string into the wire shape: the underlying connection string's
     * own properties plus {@code Type} and {@code ExcludedDatabases}.
     * @param mapper the mapper to build the tree with
     * @return the flattened JSON object
     */
    public ObjectNode toJson(ObjectMapper mapper) {
        ObjectNode json = connectionString != null
                ? (ObjectNode) mapper.valueToTree(connectionString)
                : mapper.createObjectNode();

        json.put("Type", SharpEnum.value(getType()));

        if (excludedDatabases == null) {
            json.putNull("ExcludedDatabases");
        } else {
            ArrayNode excluded = json.putArray("ExcludedDatabases");
            for (String database : excludedDatabases) {
                excluded.add(database);
            }
        }

        return json;
    }

    static ConnectionString deserializeConnectionString(ObjectMapper mapper, JsonNode node, String type) throws IOException {
        if (type == null) {
            throw new IllegalArgumentException("Connection string type must not be null");
        }

        switch (type) {
            case "Raven":
                return mapper.treeToValue(node, RavenConnectionString.class);
            case "Sql":
                return mapper.treeToValue(node, SqlConnectionString.class);
            case "Olap":
                return mapper.treeToValue(node, OlapConnectionString.class);
            case "ElasticSearch":
                return mapper.treeToValue(node, ElasticSearchConnectionString.class);
            case "Queue":
                return mapper.treeToValue(node, QueueConnectionString.class);
            case "Ai":
                return mapper.treeToValue(node, AiConnectionString.class);
            default:
                throw new IllegalArgumentException("Unknown connection string type: " + type);
        }
    }

    public static class ServerWideConnectionStringSerializer extends JsonSerializer<ServerWideConnectionString> {
        @Override
        public void serialize(ServerWideConnectionString value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            if (value == null) {
                gen.writeNull();
                return;
            }

            gen.writeTree(value.toJson(JsonExtensions.getDefaultMapper()));
        }
    }

    public static class ServerWideConnectionStringDeserializer extends JsonDeserializer<ServerWideConnectionString> {
        @Override
        public ServerWideConnectionString deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            JsonNode node = p.readValueAsTree();
            if (node == null || node.isNull()) {
                return null;
            }

            JsonNode typeNode = node.get("Type");
            if (typeNode == null || typeNode.isNull()) {
                return null;
            }

            ObjectMapper mapper = JsonExtensions.getDefaultMapper();

            ServerWideConnectionString result = new ServerWideConnectionString();
            result.setConnectionString(deserializeConnectionString(mapper, node, typeNode.asText()));

            JsonNode excludedNode = node.get("ExcludedDatabases");
            if (excludedNode != null && excludedNode.isArray()) {
                String[] excludedDatabases = new String[excludedNode.size()];
                for (int i = 0; i < excludedNode.size(); i++) {
                    JsonNode entry = excludedNode.get(i);
                    excludedDatabases[i] = entry == null || entry.isNull() ? null : entry.asText();
                }
                result.setExcludedDatabases(excludedDatabases);
            }

            return result;
        }
    }
}
