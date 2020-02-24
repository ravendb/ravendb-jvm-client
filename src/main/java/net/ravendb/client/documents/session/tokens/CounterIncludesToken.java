package net.ravendb.client.documents.session.tokens;

import org.apache.commons.lang3.StringUtils;

public class CounterIncludesToken extends QueryToken {

    private String _sourcePath;
    private final String _counterName;
    private final boolean _all;

    private CounterIncludesToken(String sourcePath, String counterName, boolean all) {
        _counterName = counterName;
        _all = all;
        _sourcePath = sourcePath;
    }

    public static CounterIncludesToken create(String sourcePath, String counterName) {
        return new CounterIncludesToken(sourcePath, counterName, false);
    }

    public static CounterIncludesToken all(String sourcePath) {
        return new CounterIncludesToken(sourcePath, null, true);
    }

    public void addAliasToPath(String alias) {
        _sourcePath = StringUtils.isEmpty(_sourcePath) ?
                alias
                : alias + "." + _sourcePath;
    }

    @Override
    public void writeTo(StringBuilder writer) {
        writer
            .append("counters(");

        if (StringUtils.isNotEmpty(_sourcePath)) {
            writer
                .append(_sourcePath);

            if (!_all) {
                writer.append(", ");
            }
        }

        if (!_all) {
            writer
                .append("'")
                .append(_counterName)
                .append("'");
        }

        writer
            .append(")");
    }
}
