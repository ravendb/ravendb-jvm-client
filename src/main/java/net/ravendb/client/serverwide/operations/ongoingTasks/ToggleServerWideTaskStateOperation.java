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
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;

public class ToggleServerWideTaskStateOperation implements IVoidServerOperation {
    private final String _name;
    private final OngoingTaskType _type;
    private final boolean _disable;

    public ToggleServerWideTaskStateOperation(String name, OngoingTaskType type, boolean disable) {
        if (name == null) {
            throw new IllegalArgumentException("Name cannot be null");
        }

        _name = name;
        _type = type;
        _disable = disable;
    }

    @Override
    public VoidRavenCommand getCommand(DocumentConventions conventions) {
        return new ToggleServerWideTaskStateCommand(_name, _type, _disable);
    }

    private static class ToggleServerWideTaskStateCommand extends VoidRavenCommand implements IRaftCommand {
        private final String _name;
        private final OngoingTaskType _type;
        private final boolean _disable;

        public ToggleServerWideTaskStateCommand(String name, OngoingTaskType type, boolean disable) {
            if (name == null) {
                throw new IllegalArgumentException("Name cannot be null");
            }

            _name = name;
            _type = type;
            _disable = disable;
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
            url.value = node.getUrl() + "/admin/configuration/server-wide/state?type=" + SharpEnum.value(_type)
                    + "&name=" + urlEncode(_name) + "&disable=" + _disable;

            return new HttpPost();
        }
    }
}
