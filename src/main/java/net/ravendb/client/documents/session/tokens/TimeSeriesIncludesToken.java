package net.ravendb.client.documents.session.tokens;

import net.ravendb.client.documents.operations.timeSeries.AbstractTimeSeriesRange;
import net.ravendb.client.documents.operations.timeSeries.TimeSeriesCountRange;
import net.ravendb.client.documents.operations.timeSeries.TimeSeriesRange;
import net.ravendb.client.documents.operations.timeSeries.TimeSeriesTimeRange;
import net.ravendb.client.primitives.NetISO8601Utils;
import net.ravendb.client.primitives.SharpEnum;
import org.apache.commons.lang3.StringUtils;

public final class TimeSeriesIncludesToken extends QueryToken {

    private String _sourcePath;
    private final AbstractTimeSeriesRange _range;

    private TimeSeriesIncludesToken(String sourcePath, AbstractTimeSeriesRange range) {
        _range = range;
        _sourcePath = sourcePath;
    }

    public static TimeSeriesIncludesToken create(String sourcePath, AbstractTimeSeriesRange range) {
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

        if (StringUtils.isNotEmpty(_range.getName())) {
            writer
                    .append("'")
                    .append(_range.getName())
                    .append("'")
                    .append(", ");
        }

        if (_range instanceof TimeSeriesRange) {
            TimeSeriesRange r = (TimeSeriesRange) _range;
            writeTo(writer, r);
        } else if (_range instanceof TimeSeriesTimeRange) {
            TimeSeriesTimeRange tr = (TimeSeriesTimeRange) _range;
            writeTo(writer, tr);
        } else if (_range instanceof TimeSeriesCountRange) {
            TimeSeriesCountRange cr = (TimeSeriesCountRange) _range;
            writeTo(writer, cr);
        } else {
            throw new IllegalArgumentException("Not supported time range type: " + _range.getClass().getSimpleName());
        }

        writer
                .append(")");
    }

    private static void writeTo(StringBuilder writer, TimeSeriesTimeRange range) {
        switch (range.getType()) {
            case LAST:
                writer
                        .append("last(");
                break;
            default:
                throw new IllegalArgumentException("Not supported time range type: " + range.getType());
        }

        writer
                .append(range.getTime().getValue())
                .append(", '")
                .append(SharpEnum.value(range.getTime().getUnit()))
                .append("')");
    }

    private static void writeTo(StringBuilder writer, TimeSeriesCountRange range) {
        switch (range.getType()) {
            case LAST:
                writer
                        .append("last(");
                break;
            default:
                throw new IllegalArgumentException("Not supported time range type: " + range.getType());
        }

        writer
                .append(range.getCount())
                .append(")");
    }

    private static void writeTo(StringBuilder writer, TimeSeriesRange range) {
        if (range.getFrom() != null) {
            writer
                    .append("'")
                    .append(NetISO8601Utils.format(range.getFrom(), true))
                    .append("'")
                    .append(", ");
        } else {
            writer.append("null,");
        }

        if (range.getTo() != null) {
            writer
                    .append("'")
                    .append(NetISO8601Utils.format(range.getTo(), true))
                    .append("'");
        } else {
            writer
                    .append("null");
        }
    }
}
