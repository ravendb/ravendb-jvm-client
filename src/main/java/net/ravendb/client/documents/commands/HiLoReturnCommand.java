package net.ravendb.client.documents.commands;

import net.ravendb.client.http.ServerNode;
import net.ravendb.client.http.VoidRavenCommand;
import net.ravendb.client.primitives.Reference;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;

public class HiLoReturnCommand extends VoidRavenCommand {

    private final String _tag;
    private final long _last;
    private final long _end;

    public HiLoReturnCommand(String tag, long last, long end) {
        if (last < 0) {
            throw new IllegalArgumentException("last is < 0");
        }

        if (end < 0) {
            throw new IllegalArgumentException("end is < 0");
        }

        if (tag == null) {
            throw new IllegalArgumentException("tag cannot be null");
        }

        _tag = tag;
        _last = last;
        _end = end;
    }

    @Override
    public HttpRequestBase createRequest(ServerNode node, Reference<String> url) {
        url.value = node.getUrl() + "/databases/" + node.getDatabase() + "/hilo/return?tag=" + _tag + "&end=" + _end + "&last=" + _last;

        return new HttpPut();
    }
}
