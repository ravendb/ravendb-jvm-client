package net.ravendb.client.documents.commands;

import net.ravendb.client.http.ServerNode;
import net.ravendb.client.http.VoidRavenCommand;
import org.apache.commons.lang3.StringUtils;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;

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
    public HttpUriRequestBase createRequest(ServerNode node) {
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

        String url = path.toString();

        return new HttpPost(url);
    }
}
