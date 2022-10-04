package net.ravendb.client.documents.commands;

import net.ravendb.client.http.ServerNode;
import net.ravendb.client.http.VoidRavenCommand;
import net.ravendb.client.primitives.Reference;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;

public class DropSubscriptionConnectionCommand extends VoidRavenCommand {
    private final String _name;
    private final String _workerId;

    public DropSubscriptionConnectionCommand(String name) {
        this(name, null);
    }

    public DropSubscriptionConnectionCommand(String name, String workerId) {
        _name = name;
        _workerId = workerId;
    }

    @Override
    public HttpRequestBase createRequest(ServerNode node, Reference<String> url) {
        StringBuilder path = new StringBuilder();
        path.append(node.getUrl())
            .append("/databases/")
            .append(node.getDatabase())
            .append("/subscriptions/drop");

        if (StringUtils.isNotEmpty(_name)) {
            path.append("?name=")
                    .append(urlEncode(_name));
        }

        if (StringUtils.isNotEmpty(_workerId)) {
            path.append("&workerId=")
                    .append(_workerId);
        }

        url.value = path.toString();

        return new HttpPost();
    }
}
