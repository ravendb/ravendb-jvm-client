package net.ravendb.client.documents.queries.spatial;

import net.ravendb.client.documents.indexes.spatial.SpatialRelation;
import net.ravendb.client.documents.indexes.spatial.SpatialUnits;
import net.ravendb.client.documents.session.tokens.ShapeToken;

import java.util.function.Function;

public class WktCriteria extends SpatialCriteria {
    private final String _shapeWkt;
    private final SpatialUnits _radiusUnits;

    public WktCriteria(String shapeWkt, SpatialRelation relation, SpatialUnits radiusUnits, double distanceErrorPct) {
        super(relation, distanceErrorPct);
        _shapeWkt = shapeWkt;
        _radiusUnits = radiusUnits;
    }

    @Override
    protected ShapeToken getShapeToken(Function<Object, String> addQueryParameter) {
        return ShapeToken.wkt(addQueryParameter.apply(_shapeWkt), _radiusUnits);
    }
}
