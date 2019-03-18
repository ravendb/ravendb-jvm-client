package net.ravendb.client.serverwide.operations;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.node.ObjectNode;
import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.http.RavenCommand;
import net.ravendb.client.http.ServerNode;
import net.ravendb.client.json.ContentProviderHttpEntity;
import net.ravendb.client.primitives.Reference;
import net.ravendb.client.serverwide.ConflictSolver;
import net.ravendb.client.serverwide.ScriptResolver;
import net.ravendb.client.util.UrlUtils;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.ContentType;

import java.io.IOException;
import java.util.Map;

public class ModifyConflictSolverOperation implements IServerOperation<ModifySolverResult> {
    private final String _database;
    private final Map<String, ScriptResolver> _collectionByScript;
    private final boolean _resolveToLatest;

    public ModifyConflictSolverOperation(String database) {
        this(database, null, false);
    }

    public ModifyConflictSolverOperation(String database, Map<String, ScriptResolver> collectionByScript) {
        this(database, collectionByScript, false);
    }

    public ModifyConflictSolverOperation(String database, Map<String, ScriptResolver> collectionByScript, boolean resolveToLatest) {
        _database = database;
        _collectionByScript = collectionByScript;
        _resolveToLatest = resolveToLatest;
    }

    @Override
    public RavenCommand<ModifySolverResult> getCommand(DocumentConventions conventions) {
        return new ModifyConflictSolverCommand(conventions, _database, this);
    }

    private static class ModifyConflictSolverCommand extends RavenCommand<ModifySolverResult> {
        private final ModifyConflictSolverOperation _solver;
        private final DocumentConventions _conventions;
        private final String _databaseName;

        public ModifyConflictSolverCommand(DocumentConventions conventions, String database, ModifyConflictSolverOperation solver) {
            super(ModifySolverResult.class);

            if (conventions == null) {
                throw new IllegalArgumentException("Conventions cannot be null");
            }
            if (database == null) {
                throw new IllegalArgumentException("Database cannot be null");
            }

            _conventions = conventions;
            _databaseName = database;
            _solver = solver;
        }

        @Override
        public HttpRequestBase createRequest(ServerNode node, Reference<String> url) {
            url.value = node.getUrl() + "/admin/replication/conflicts/solver?name=" + UrlUtils.escapeDataString(_databaseName);

            HttpPost request = new HttpPost();

            request.setEntity(new ContentProviderHttpEntity(outputStream -> {
                try (JsonGenerator generator = mapper.getFactory().createGenerator(outputStream)) {
                    ConflictSolver solver = new ConflictSolver();
                    solver.setResolveToLatest(_solver._resolveToLatest);
                    solver.setResolveByCollection(_solver._collectionByScript);
                    ObjectNode config = mapper.valueToTree(solver);
                    generator.writeTree(config);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }, ContentType.APPLICATION_JSON));

            return request;
        }

        @Override
        public void setResponse(String response, boolean fromCache) throws IOException {
            if (response == null) {
                throwInvalidResponse();
            }

            result = mapper.readValue(response, resultClass);
        }

        @Override
        public boolean isReadRequest() {
            return false;
        }
    }
}
