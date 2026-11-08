package net.ravendb.client.serverwide.sharding;

import com.fasterxml.jackson.core.JsonGenerator;
import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.http.IRaftCommand;
import net.ravendb.client.http.ServerNode;
import net.ravendb.client.http.VoidRavenCommand;
import net.ravendb.client.json.ContentProviderHttpEntity;
import net.ravendb.client.util.RaftIdGenerator;
import org.apache.hc.client5.http.classic.methods.HttpDelete;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpPut;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.core5.http.ContentType;

import java.io.IOException;

abstract class PrefixedShardingCommand extends VoidRavenCommand implements IRaftCommand {

    private final DocumentConventions _conventions;
    private final PrefixedShardingSetting _setting;
    private final String _raftUniqueRequestId = RaftIdGenerator.newId();

    protected abstract PrefixedCommandType getCommandType();

    protected PrefixedShardingCommand(DocumentConventions conventions, PrefixedShardingSetting setting) {
        _conventions = conventions;
        _setting = setting;
    }

    @Override
    public boolean isReadRequest() {
        return false;
    }

    @Override
    public HttpUriRequestBase createRequest(ServerNode node) {
        String url = node.getUrl() + "/databases/" + node.getDatabase() + "/admin/sharding/prefixed";

        HttpUriRequestBase request;
        switch (getCommandType()) {
            case ADD:
                request = new HttpPut(url);
                break;
            case DELETE:
                request = new HttpDelete(url);
                break;
            case UPDATE:
                request = new HttpPost(url);
                break;
            default:
                throw new IllegalArgumentException("Unexpected command type: " + getCommandType());
        }

        request.setEntity(new ContentProviderHttpEntity(outputStream -> {
            try (JsonGenerator generator = createSafeJsonGenerator(outputStream)) {
                generator.writeObject(_setting);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }, ContentType.APPLICATION_JSON, _conventions));

        return request;
    }

    @Override
    public String getRaftUniqueRequestId() {
        return _raftUniqueRequestId;
    }

    enum PrefixedCommandType {
        ADD,
        DELETE,
        UPDATE
    }
}
