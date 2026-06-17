package net.ravendb.client.documents.session.tokens;

import net.ravendb.client.Constants;
import net.ravendb.client.documents.session.NullsOrdering;
import net.ravendb.client.documents.session.OrderingType;

public class OrderByToken extends QueryToken {

    private final String _fieldName;
    private final boolean _descending;
    private final String _sorterName;
    private final OrderingType _ordering;
    private final NullsOrdering _nullsOrdering;
    private final boolean _isMethodField;

    private OrderByToken(String fieldName, boolean descending, OrderingType ordering, NullsOrdering nullsOrdering, boolean isMethodField) {
        _fieldName = fieldName;
        _descending = descending;
        _ordering = ordering;
        _nullsOrdering = nullsOrdering != null ? nullsOrdering : NullsOrdering.DEFAULT;
        _sorterName = null;
        _isMethodField = isMethodField;
    }

    private OrderByToken(String fieldName, boolean descending, String sorterName, boolean isMethodField) {
        _fieldName = fieldName;
        _descending = descending;
        _sorterName = sorterName;
        _ordering = null;
        _nullsOrdering = NullsOrdering.DEFAULT;
        _isMethodField = isMethodField;
    }

    public static final OrderByToken random = new OrderByToken("random()", false, OrderingType.STRING, NullsOrdering.DEFAULT, true);

    public static final OrderByToken scoreAscending = new OrderByToken("score()", false, OrderingType.STRING, NullsOrdering.DEFAULT, true);

    public static final OrderByToken scoreDescending = new OrderByToken("score()", true, OrderingType.STRING, NullsOrdering.DEFAULT, true);

    public static OrderByToken createDistanceAscending(String fieldName, String latitudeParameterName, String longitudeParameterName, String roundFactorParameterName) {
        return createDistanceAscending(fieldName, latitudeParameterName, longitudeParameterName, roundFactorParameterName, NullsOrdering.DEFAULT);
    }

    public static OrderByToken createDistanceAscending(String fieldName, String latitudeParameterName, String longitudeParameterName, String roundFactorParameterName, NullsOrdering nulls) {
        return new OrderByToken("spatial.distance(" + fieldName + ", spatial.point($" + latitudeParameterName + ", $" + longitudeParameterName + ")" + (roundFactorParameterName == null ? "" : ", $" + roundFactorParameterName) + ")",
                false, OrderingType.STRING, nulls, true);
    }

    public static OrderByToken createDistanceAscending(String fieldName, String shapeWktParameterName, String roundFactorParameterName) {
        return createDistanceAscending(fieldName, shapeWktParameterName, roundFactorParameterName, NullsOrdering.DEFAULT);
    }

    public static OrderByToken createDistanceAscending(String fieldName, String shapeWktParameterName, String roundFactorParameterName, NullsOrdering nulls) {
        return new OrderByToken("spatial.distance(" + fieldName + ", spatial.wkt($" + shapeWktParameterName + ")" + (roundFactorParameterName == null ? "" : ", $" + roundFactorParameterName) + ")",
                false, OrderingType.STRING, nulls, true);
    }

    public static OrderByToken createDistanceDescending(String fieldName, String latitudeParameterName, String longitudeParameterName, String roundFactorParameterName) {
        return createDistanceDescending(fieldName, latitudeParameterName, longitudeParameterName, roundFactorParameterName, NullsOrdering.DEFAULT);
    }

    public static OrderByToken createDistanceDescending(String fieldName, String latitudeParameterName, String longitudeParameterName, String roundFactorParameterName, NullsOrdering nulls) {
        return new OrderByToken("spatial.distance(" + fieldName + ", spatial.point($" + latitudeParameterName + ", $" + longitudeParameterName + ")" + (roundFactorParameterName == null ? "" : ", $" + roundFactorParameterName) + ")",
                true, OrderingType.STRING, nulls, true);
    }

    public static OrderByToken createDistanceDescending(String fieldName, String shapeWktParameterName, String roundFactorParameterName) {
        return createDistanceDescending(fieldName, shapeWktParameterName, roundFactorParameterName, NullsOrdering.DEFAULT);
    }

    public static OrderByToken createDistanceDescending(String fieldName, String shapeWktParameterName, String roundFactorParameterName, NullsOrdering nulls) {
        return new OrderByToken("spatial.distance(" + fieldName + ", spatial.wkt($" + shapeWktParameterName + ")" + (roundFactorParameterName == null ? "" : ", $" + roundFactorParameterName) + ")", true, OrderingType.STRING, nulls, true);
    }

    public static OrderByToken createRandom(String seed) {
        if (seed == null) {
            throw new IllegalArgumentException("seed cannot be null");
        }

        return new OrderByToken("random('" + seed.replaceAll("'", "''") + "')", false, OrderingType.STRING, NullsOrdering.DEFAULT, true);
    }

    public static OrderByToken createAscending(String fieldName, String sorterName) {
        return new OrderByToken(fieldName, false, sorterName, false);
    }

    public static OrderByToken createAscending(String fieldName, OrderingType ordering) {
        return createAscending(fieldName, ordering, NullsOrdering.DEFAULT);
    }

    public static OrderByToken createAscending(String fieldName, OrderingType ordering, NullsOrdering nulls) {
        return new OrderByToken(fieldName, false, ordering, nulls, false);
    }

    public static OrderByToken createDescending(String fieldName, String sorterName) {
        return new OrderByToken(fieldName, true, sorterName, false);
    }

    public static OrderByToken createDescending(String fieldName, OrderingType ordering) {
        return createDescending(fieldName, ordering, NullsOrdering.DEFAULT);
    }

    public static OrderByToken createDescending(String fieldName, OrderingType ordering, NullsOrdering nulls) {
        return new OrderByToken(fieldName, true, ordering, nulls, false);
    }

    @Override
    public void writeTo(StringBuilder writer) {
        if (_sorterName != null) {
            writer
                .append("custom(");
        }

        writeField(writer, _fieldName);

        if (_sorterName != null) {
            writer
                .append(", '")
                .append(_sorterName)
                .append("')");
        } else {
            switch (_ordering) {
                case LONG:
                    writer.append(" as long");
                    break;
                case DOUBLE:
                    writer.append(" as double");
                    break;
                case ALPHA_NUMERIC:
                    writer.append(" as alphaNumeric");
                    break;
            }
        }

        if (_descending) { // we only add this if we have to, ASC is the default and reads nicer
            writer.append(" desc");
        }

        switch (_nullsOrdering) {
            case FIRST:
                writer.append(" nulls first");
                break;
            case LAST:
                writer.append(" nulls last");
                break;
        }
    }

    public OrderByToken addAlias(String alias) {
        if (Constants.Documents.Indexing.Fields.DOCUMENT_ID_FIELD_NAME.equals(_fieldName)) {
            return this;
        }

        if (_isMethodField) { // we must not alias RQL methods
            return this;
        }

        String aliasedName = alias + "." + _fieldName;
        if (_sorterName != null) {
            return new OrderByToken(aliasedName, _descending, _sorterName, false);
        } else {
            return new OrderByToken(aliasedName, _descending, _ordering, _nullsOrdering, false);
        }
    }
}
