package net.ravendb.client.documents.session.tokens;

import net.ravendb.client.primitives.NetISO8601Utils;
import org.apache.commons.lang3.StringUtils;

import java.util.Date;

public class RevisionIncludesToken extends QueryToken {

    private final String _dateTime;
    private final String _path;

    private RevisionIncludesToken(Date dateTime) {
        _dateTime = NetISO8601Utils.format(dateTime, true);
        _path = null;
    }

    private RevisionIncludesToken(String path) {
        _dateTime = null;
        _path = path;
    }

    public static RevisionIncludesToken create(Date dateTime) {
        return new RevisionIncludesToken(dateTime);
    }

    public static RevisionIncludesToken create(String path) {
        return new RevisionIncludesToken(path);
    }

    @Override
    public void writeTo(StringBuilder writer) {
        writer
                .append("revisions('");

        if (_dateTime != null) {
            writer.append(_dateTime);
        } else if (StringUtils.isNotEmpty(_path)) {
            writer.append(_path);
        }
        writer.append("')");
    }
}
