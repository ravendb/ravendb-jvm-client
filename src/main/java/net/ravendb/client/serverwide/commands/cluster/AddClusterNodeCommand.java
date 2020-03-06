package net.ravendb.client.serverwide.commands.cluster;

import net.ravendb.client.http.IRaftCommand;
import net.ravendb.client.http.ServerNode;
import net.ravendb.client.http.VoidRavenCommand;
import net.ravendb.client.primitives.Reference;
import net.ravendb.client.util.RaftIdGenerator;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;

public class AddClusterNodeCommand extends VoidRavenCommand implements IRaftCommand {

    private final String _url;
    private final String _tag;
    private final boolean _watcher;

    public AddClusterNodeCommand(String url) {
        this(url, null, false);
    }

    public AddClusterNodeCommand(String url, String tag) {
        this(url, tag, false);
    }

    public AddClusterNodeCommand(String url, String tag, boolean watcher) {
        _url = url;
        _tag = tag;
        _watcher = watcher;
    }

    @Override
    public boolean isReadRequest() {
        return false;
    }

    @Override
    public HttpRequestBase createRequest(ServerNode node, Reference<String> url) {
        url.value = node.getUrl() + "/admin/cluster/node?url=" + urlEncode(_url) + "&watcher=" + _watcher;

        if (StringUtils.isNotBlank(_tag)) {
            url.value += "&tag=" + _tag;
        }

        return new HttpPut();
    }

    @Override
    public String getRaftUniqueRequestId() {
        return RaftIdGenerator.newId();
    }
}
