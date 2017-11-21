package net.ravendb.client.documents.queries.spatial;

import net.ravendb.client.documents.indexes.spatial.SpatialRelation;
import net.ravendb.client.documents.session.tokens.QueryToken;
import net.ravendb.client.documents.session.tokens.ShapeToken;
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

        QueryToken relationToken;

        switch (_relation) {
            case WITHIN:
                relationToken = WhereToken.within(fieldName, shapeToken, _distanceErrorPct);
                break;
            case CONTAINS:
                relationToken = WhereToken.contains(fieldName, shapeToken, _distanceErrorPct);
                break;
            case DISJOINT:
                relationToken = WhereToken.disjoint(fieldName, shapeToken, _distanceErrorPct);
                break;
            case INTERSECTS:
                relationToken = WhereToken.intersects(fieldName, shapeToken, _distanceErrorPct);
                break;
            default:
                throw new IllegalArgumentException();
        }

        return relationToken;
    }
}
