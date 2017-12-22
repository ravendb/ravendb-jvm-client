package net.ravendb.client.documents.queries.spatial;

import net.ravendb.client.documents.indexes.spatial.SpatialRelation;
import net.ravendb.client.documents.indexes.spatial.SpatialUnits;
import net.ravendb.client.documents.session.tokens.ShapeToken;

import java.util.function.Function;

public class CircleCriteria extends SpatialCriteria {

    private final double _radius;
    private final double _latitude;
    private final double _longitude;
    private final SpatialUnits _radiusUnits;

    public CircleCriteria(double radius, double latitude, double longitude, SpatialUnits radiusUnits, SpatialRelation relation, double distErrorPercent) {
        super(relation, distErrorPercent);

        _radius = radius;
        _latitude = latitude;
        _longitude = longitude;
        _radiusUnits = radiusUnits;
    }

    @Override
    protected ShapeToken getShapeToken(Function<Object, String> addQueryParameter) {
        return ShapeToken.circle(addQueryParameter.apply(_radius), addQueryParameter.apply(_latitude),
                addQueryParameter.apply(_longitude), _radiusUnits);
    }
}
