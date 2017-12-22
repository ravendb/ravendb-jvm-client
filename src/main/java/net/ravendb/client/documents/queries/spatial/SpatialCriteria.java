package net.ravendb.client.documents.queries.spatial;

import net.ravendb.client.documents.indexes.spatial.SpatialRelation;
import net.ravendb.client.documents.session.tokens.QueryToken;
import net.ravendb.client.documents.session.tokens.ShapeToken;
import net.ravendb.client.documents.session.tokens.WhereOperator;
import net.ravendb.client.documents.session.tokens.WhereToken;

import java.util.function.Function;

public abstract class SpatialCriteria {

    private final SpatialRelation _relation;
    private final double _distanceErrorPct;

    protected SpatialCriteria(SpatialRelation relation, double distanceErrorPct) {
        _relation = relation;
        _distanceErrorPct = distanceErrorPct;
    }

    protected abstract ShapeToken getShapeToken(Function<Object, String> addQueryParameter);

    public QueryToken toQueryToken(String fieldName, Function<Object, String> addQueryParameter) {
        ShapeToken shapeToken = getShapeToken(addQueryParameter);

        WhereOperator whereOperator;

        switch (_relation) {
            case WITHIN:
                whereOperator = WhereOperator.SPATIAL_WITHIN;
                break;
            case CONTAINS:
                whereOperator = WhereOperator.SPATIAL_CONTAINS;
                break;
            case DISJOINT:
                whereOperator = WhereOperator.SPATIAL_DISJOINT;
                break;
            case INTERSECTS:
                whereOperator = WhereOperator.SPATIAL_INTERSECTS;
                break;
            default:
                throw new IllegalArgumentException();
        }

        return WhereToken.create(whereOperator, fieldName, null, new WhereToken.WhereOptions(shapeToken, _distanceErrorPct));
    }
}
