package net.ravendb.client.documents.session.tokens;

import net.ravendb.client.documents.operations.timeSeries.TimeSeriesRange;
import net.ravendb.client.primitives.NetISO8601Utils;
import org.apache.commons.lang3.StringUtils;

public class TimeSeriesIncludesToken extends QueryToken {

    private String _sourcePath;
    private final TimeSeriesRange _range;

    private TimeSeriesIncludesToken(String sourcePath, TimeSeriesRange range) {
        _range = range;
        _sourcePath = sourcePath;
    }

    public static TimeSeriesIncludesToken create(String sourcePath, TimeSeriesRange range) {
        return new TimeSeriesIncludesToken(sourcePath, range);
    }

    public void addAliasToPath(String alias) {
        _sourcePath = StringUtils.isEmpty(_sourcePath)
                ? alias
                : alias + "." + _sourcePath;
    }

    @Override
    public void writeTo(StringBuilder writer) {
        writer.append("timeseries(");

        if (StringUtils.isNotEmpty(_sourcePath)) {
            writer
                    .append(_sourcePath)
                    .append(", ");
        }

        writer
                .append("'")
                .append(_range.getName())
                .append("'")
                .append(", ");

        if (_range.getFrom() != null) {
            writer
                    .append("'")
                    .append(NetISO8601Utils.format(_range.getFrom(), true))
                    .append("'")
                    .append(", ");
        } else {
            writer
                    .append("null,");
        }

        if (_range.getTo() != null) {
            writer
                    .append("'")
                    .append(NetISO8601Utils.format(_range.getTo(), true))
                    .append("'");
        } else {
            writer
                    .append("null");
        }

        writer
                .append(")");
    }
}
