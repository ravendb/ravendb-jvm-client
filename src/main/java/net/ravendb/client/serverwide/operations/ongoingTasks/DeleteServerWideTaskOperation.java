package net.ravendb.client.serverwide.operations.ongoingTasks;

import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.operations.ongoingTasks.OngoingTaskType;
import net.ravendb.client.http.IRaftCommand;
import net.ravendb.client.http.ServerNode;
import net.ravendb.client.http.VoidRavenCommand;
import net.ravendb.client.primitives.Reference;
import net.ravendb.client.primitives.SharpEnum;
import net.ravendb.client.serverwide.operations.IVoidServerOperation;
import net.ravendb.client.util.RaftIdGenerator;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpRequestBase;

public class DeleteServerWideTaskOperation implements IVoidServerOperation {

    private final String _name;
    private final OngoingTaskType _type;

    public DeleteServerWideTaskOperation(String name, OngoingTaskType type) {
        if (name == null) {
            throw new IllegalArgumentException("Name cannot be null");
        }
        _name = name;
        _type = type;
    }

    @Override
    public VoidRavenCommand getCommand(DocumentConventions conventions) {
        return new DeleteServerWideTaskCommand(_name, _type);
    }

    private static class DeleteServerWideTaskCommand extends VoidRavenCommand implements IRaftCommand {
        private final String _name;
        private final OngoingTaskType _type;

        public DeleteServerWideTaskCommand(String name, OngoingTaskType type) {
            if (name == null) {
                throw new IllegalArgumentException("Name cannot be null");
            }

            _name = name;
            _type = type;
        }

        @Override
        public boolean isReadRequest() {
            return false;
        }

        @Override
        public String getRaftUniqueRequestId() {
            return RaftIdGenerator.newId();
        }

        @Override
        public HttpRequestBase createRequest(ServerNode node, Reference<String> url) {
            url.value = node.getUrl() + "/admin/configuration/server-wide/task?type=" + SharpEnum.value(_type) + "&name=" + urlEncode(_name);

            return new HttpDelete();
        }
    }
}
