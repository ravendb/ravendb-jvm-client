package net.ravendb.client.documents.commands;

import net.ravendb.client.http.ServerNode;
import net.ravendb.client.http.VoidRavenCommand;
import org.apache.hc.client5.http.classic.methods.HttpPut;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;

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
    public HttpUriRequestBase createRequest(ServerNode node) {
        String url = node.getUrl() + "/databases/"
                + node.getDatabase() + "/hilo/return?tag="
                + _tag + "&end=" + _end + "&last=" + _last;

        return new HttpPut(url);
    }
}
