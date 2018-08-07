package net.ravendb.client.documents.session.tokens;

import net.ravendb.client.documents.indexes.spatial.SpatialUnits;

public class ShapeToken extends QueryToken {
    private final String _shape;

    private ShapeToken(String shape) {
        _shape = shape;
    }

    public static ShapeToken circle(String radiusParameterName, String latitudeParameterName, String longitudeParameterName, SpatialUnits radiusUnits) {
        if (radiusUnits == null) {
            return new ShapeToken("spatial.circle($" + radiusParameterName + ", $" + latitudeParameterName + ", $" + longitudeParameterName + ")");
        }

        if (radiusUnits == SpatialUnits.KILOMETERS) {
            return new ShapeToken("spatial.circle($" + radiusParameterName + ", $" + latitudeParameterName + ", $" + longitudeParameterName + ", 'Kilometers')");
        }
        return new ShapeToken("spatial.circle($" + radiusParameterName + ", $" + latitudeParameterName + ", $" + longitudeParameterName + ", 'Miles')");
    }

    public static ShapeToken wkt(String shapeWktParameterName, SpatialUnits units) {
        if (units == null) {
            return new ShapeToken("spatial.wkt($" + shapeWktParameterName + ")");
        }

        if (units == SpatialUnits.KILOMETERS) {
            return new ShapeToken("spatial.wkt($" + shapeWktParameterName + ", 'Kilometers')");
        }
        return new ShapeToken("spatial.wkt($" + shapeWktParameterName + ", 'Miles')");
    }

    @Override
    public void writeTo(StringBuilder writer) {
        writer.append(_shape);
    }
}
