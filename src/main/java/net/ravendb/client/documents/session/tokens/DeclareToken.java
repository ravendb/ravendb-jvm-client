package net.ravendb.client.documents.session.tokens;

public class DeclareToken extends QueryToken {

    private final String _name;
    private final String _parameters;
    private final String _body;
    private final boolean _timeSeries;

    private DeclareToken(String name, String body, String parameters, boolean timeSeries) {
        _name = name;
        _body = body;
        _parameters = parameters;
        _timeSeries = timeSeries;
    }

    public static DeclareToken createFunction(String name, String body) {
        return createFunction(name, body, null);
    }

    public static DeclareToken createFunction(String name, String body, String parameters) {
        return new DeclareToken(name, body, parameters, false);
    }

    public static DeclareToken createTimeSeries(String name, String body) {
        return createTimeSeries(name, body, null);
    }

    public static DeclareToken createTimeSeries(String name, String body, String parameters) {
        return new DeclareToken(name, body, parameters, true);
    }

    @Override
    public void writeTo(StringBuilder writer) {
        writer
                .append("declare ")
                .append(_timeSeries ? "timeseries " : "function ")
                .append(_name)
                .append("(")
                .append(_parameters)
                .append(") ")
                .append("{")
                .append(System.lineSeparator())
                .append(_body)
                .append(System.lineSeparator())
                .append("}")
                .append(System.lineSeparator());
    }
}
