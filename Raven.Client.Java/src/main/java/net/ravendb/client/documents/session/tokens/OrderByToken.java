package net.ravendb.client.documents.session.tokens;

import net.ravendb.client.documents.session.OrderingType;

public class OrderByToken extends QueryToken {

    private final String _fieldName;
    private final boolean _descending;
    private final OrderingType _ordering;

    private OrderByToken(String fieldName, boolean descending, OrderingType ordering) {
        _fieldName = fieldName;
        _descending = descending;
        _ordering = ordering;
    }

    public static OrderByToken random = new OrderByToken("random()", false, OrderingType.STRING);

    public static OrderByToken scoreAscending = new OrderByToken("score()", false, OrderingType.STRING);

    public static OrderByToken scoreDescending = new OrderByToken("score()", true, OrderingType.STRING);

    public static OrderByToken createDistanceAscending(String fieldName, String latitudeParameterName, String longitudeParameterName) {
        return new OrderByToken("spatial.distance(" + fieldName + ", point(" + latitudeParameterName + ", " + longitudeParameterName + "))", false, OrderingType.STRING);
    }

    public static OrderByToken createDistanceAscending(String fieldName, String shapeWktParameterName) {
        return new OrderByToken("spatial.distance(" + fieldName + ", wkt(" + shapeWktParameterName + "))", false, OrderingType.STRING);
    }

    public static OrderByToken createDistanceDescending(String fieldName, String latitudeParameterName, String longitudeParameterName) {
        return new OrderByToken("spatial.distance(" + fieldName + ", point(" + latitudeParameterName + ", " + longitudeParameterName + "))", true, OrderingType.STRING);
    }

    public static OrderByToken createDistanceDescending(String fieldName, String shapeWktParameterName) {
        return new OrderByToken("spatial.distance(" + fieldName + ", wkt(" + shapeWktParameterName + "))", true, OrderingType.STRING);
    }

    public static OrderByToken createRandom(String seed) {
        if (seed == null) {
            throw new IllegalArgumentException("seed cannot be null");
        }

        return new OrderByToken("random(" + seed.replaceAll("'", "''") + "')", false, OrderingType.STRING);
    }

    public static OrderByToken createAscending(String fieldName, OrderingType ordering) {
        return new OrderByToken(fieldName, false, ordering);
    }

    public static OrderByToken createDescending(String fieldName, OrderingType ordering) {
        return new OrderByToken(fieldName, true, ordering);
    }

    @Override
    public void writeTo(StringBuilder writer) {
        writeField(writer, _fieldName);

        switch (_ordering) {
            case LONG:
                writer.append(" AS long");
                break;
            case DOUBLE:
                writer.append(" AS double");
                break;
            case ALPHA_NUMERIC:
                writer.append(" AS alphaNumeric");
                break;
        }

        if (_descending) { // we only add this if we have to, ASC is the default and reads nicer
            writer.append(" DESC");
        }
    }
}
