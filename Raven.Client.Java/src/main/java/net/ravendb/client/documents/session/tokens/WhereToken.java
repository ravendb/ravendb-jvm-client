package net.ravendb.client.documents.session.tokens;

import net.ravendb.client.Constants;
import net.ravendb.client.documents.queries.SearchOperator;

public class WhereToken extends QueryToken {

    private WhereToken() {
    }

    private String fieldName;
    private WhereOperator whereOperator;
    private SearchOperator searchOperator;
    private String parameterName;
    private String fromParameterName;
    private String toParameterName;
    private Double boost;
    private Double fuzzy;
    private Integer proximity;
    private boolean exact;
    private ShapeToken whereShape;
    private double distanceErrorPct;

    public String getFieldName() {
        return fieldName;
    }

    public WhereOperator getWhereOperator() {
        return whereOperator;
    }

    public String getParameterName() {
        return parameterName;
    }

    public String getFromParameterName() {
        return fromParameterName;
    }

    public String getToParameterName() {
        return toParameterName;
    }

    public Double getBoost() {
        return boost;
    }

    public void setBoost(Double boost) {
        this.boost = boost;
    }

    public Double getFuzzy() {
        return fuzzy;
    }

    public void setFuzzy(Double fuzzy) {
        this.fuzzy = fuzzy;
    }

    public Integer getProximity() {
        return proximity;
    }

    public void setProximity(Integer proximity) {
        this.proximity = proximity;
    }

    public boolean isExact() {
        return exact;
    }

    public SearchOperator getSearchOperator() {
        return searchOperator;
    }

    public ShapeToken getWhereShape() {
        return whereShape;
    }

    public void setWhereShape(ShapeToken whereShape) {
        this.whereShape = whereShape;
    }

    public double getDistanceErrorPct() {
        return distanceErrorPct;
    }

    public void setDistanceErrorPct(double distanceErrorPct) {
        this.distanceErrorPct = distanceErrorPct;
    }

    public static WhereToken equals(String fieldName, String parameterName, boolean exact) {
        WhereToken token = new WhereToken();
        token.fieldName = fieldName;
        token.parameterName = parameterName;
        token.whereOperator = WhereOperator.EQUALS;
        token.exact = exact;
        return token;
    }

    public static WhereToken notEquals(String fieldName, String parameterName, boolean exact) {
        WhereToken token = new WhereToken();
        token.fieldName = fieldName;
        token.parameterName = parameterName;
        token.whereOperator = WhereOperator.NOT_EQUALS;
        token.exact = exact;
        return token;
    }

    public static WhereToken startsWith(String fieldName, String parameterName, boolean exact) {
        WhereToken token = new WhereToken();
        token.fieldName = fieldName;
        token.parameterName = parameterName;
        token.whereOperator = WhereOperator.STARTS_WITH;
        token.exact = exact;
        return token;
    }

    public static WhereToken endsWith(String fieldName, String parameterName, boolean exact) {
        WhereToken token = new WhereToken();
        token.fieldName = fieldName;
        token.parameterName = parameterName;
        token.whereOperator = WhereOperator.ENDS_WITH;
        token.exact = exact;
        return token;
    }

    public static WhereToken greaterThan(String fieldName, String parameterName, boolean exact) {
        WhereToken token = new WhereToken();
        token.fieldName = fieldName;
        token.parameterName = parameterName;
        token.whereOperator = WhereOperator.GREATER_THAN;
        token.exact = exact;
        return token;
    }

    public static WhereToken greaterThanOrEqual(String fieldName, String parameterName, boolean exact) {
        WhereToken token = new WhereToken();
        token.fieldName = fieldName;
        token.parameterName = parameterName;
        token.whereOperator = WhereOperator.GREATER_THAN_OR_EQUAL;
        token.exact = exact;
        return token;
    }

    public static WhereToken lessThan(String fieldName, String parameterName, boolean exact) {
        WhereToken token = new WhereToken();
        token.fieldName = fieldName;
        token.parameterName = parameterName;
        token.whereOperator = WhereOperator.LESS_THAN;
        token.exact = exact;
        return token;
    }

    public static WhereToken lessThanOrEqual(String fieldName, String parameterName, boolean exact) {
        WhereToken token = new WhereToken();
        token.fieldName = fieldName;
        token.parameterName = parameterName;
        token.whereOperator = WhereOperator.LESS_THAN_OR_EQUAL;
        token.exact = exact;
        return token;
    }

    public static WhereToken in(String fieldName, String parameterName, boolean exact) {
        WhereToken token = new WhereToken();
        token.fieldName = fieldName;
        token.parameterName = parameterName;
        token.whereOperator = WhereOperator.IN;
        token.exact = exact;
        return token;
    }

    public static WhereToken allIn(String fieldName, String parameterName) {
        WhereToken token = new WhereToken();
        token.fieldName = fieldName;
        token.parameterName = parameterName;
        token.whereOperator = WhereOperator.ALL_IN;
        return token;
    }

    public static WhereToken between(String fieldName, String fromParameterName, String toParameterName, boolean exact) {
        WhereToken token = new WhereToken();
        token.fieldName = fieldName;
        token.fromParameterName = fromParameterName;
        token.toParameterName = toParameterName;
        token.whereOperator = WhereOperator.BETWEEN;
        token.exact = exact;
        return token;
    }

    public static WhereToken search(String fieldName, String parameterName, SearchOperator op) {
        WhereToken token = new WhereToken();
        token.fieldName = fieldName;
        token.parameterName = parameterName;
        token.whereOperator = WhereOperator.SEARCH;
        token.searchOperator = op;
        return token;
    }

    public static WhereToken cmpXchg(String fieldName, String parameterName, SearchOperator op) {
        WhereToken token = new WhereToken();
        token.fieldName = fieldName;
        token.parameterName = parameterName;
        token.whereOperator = WhereOperator.CMP_X_CHG_MATCH;
        token.searchOperator = op;
        return token;
    }

    public static WhereToken lucene(String fieldName, String parameterName) {
        WhereToken token = new WhereToken();
        token.fieldName = fieldName;
        token.parameterName = parameterName;
        token.whereOperator = WhereOperator.LUCENE;
        return token;
    }

    public static WhereToken exists(String fieldName) {
        WhereToken token = new WhereToken();
        token.fieldName = fieldName;
        token.parameterName = null;
        token.whereOperator = WhereOperator.EXISTS;
        return token;
    }

    public static QueryToken within(String fieldName, ShapeToken shape, double distanceErrorPct) {
        WhereToken token = new WhereToken();
        token.fieldName = fieldName;
        token.parameterName = null;
        token.whereOperator = WhereOperator.WITHIN;
        token.whereShape = shape;
        token.distanceErrorPct = distanceErrorPct;
        return token;
    }

    public static QueryToken contains(String fieldName, ShapeToken shape, double distanceErrorPct) {
        WhereToken token = new WhereToken();
        token.fieldName = fieldName;
        token.parameterName = null;
        token.whereOperator = WhereOperator.CONTAINS;
        token.whereShape = shape;
        token.distanceErrorPct = distanceErrorPct;
        return token;
    }

    public static QueryToken disjoint(String fieldName, ShapeToken shape, double distanceErrorPct) {
        WhereToken token = new WhereToken();
        token.fieldName = fieldName;
        token.parameterName = null;
        token.whereOperator = WhereOperator.DISJOINT;
        token.whereShape = shape;
        token.distanceErrorPct = distanceErrorPct;
        return token;
    }

    public static QueryToken intersects(String fieldNam, ShapeToken shape, double distanceErrorPct) {
        WhereToken token = new WhereToken();
        token.fieldName = fieldNam;
        token.parameterName = null;
        token.whereOperator = WhereOperator.INTERSECTS;
        token.whereShape = shape;
        token.distanceErrorPct = distanceErrorPct;
        return token;
    }

    public static QueryToken regex(String fieldName, String parameterName) {
        WhereToken token = new WhereToken();
        token.fieldName = fieldName;
        token.parameterName = parameterName;
        token.whereOperator = WhereOperator.REGEX;
        return token;
    }

    public void addAlias(String alias) {
        if ("id()".equals(fieldName)) {
            return;
        }
        fieldName = alias + "." + fieldName;
    }

    @Override
    public void writeTo(StringBuilder writer) {
        if (boost != null) {
            writer.append("boost(");
        }

        if (fuzzy != null) {
            writer.append("fuzzy(");
        }

        if (proximity != null) {
            writer.append("proximity(");
        }

        if (exact) {
            writer.append("exact(");
        }

        switch (whereOperator) {
            case SEARCH:
                writer.append("search(");
                break;
            case LUCENE:
                writer.append("lucene(");
                break;
            case STARTS_WITH:
                writer.append("startsWith(");
                break;
            case ENDS_WITH:
                writer.append("endsWith(");
                break;
            case EXISTS:
                writer.append("exists(");
                break;
            case WITHIN:
                writer.append("within(");
                break;
            case CONTAINS:
                writer.append("contains(");
                break;
            case DISJOINT:
                writer.append("disjoint(");
                break;
            case INTERSECTS:
                writer.append("intersects(");
                break;
            case REGEX:
                writer.append("regex(");
                break;
            case CMP_X_CHG_MATCH:
                writer.append("cmpxchg.match(");
        }

        writeField(writer, fieldName);

        switch (whereOperator) {
            case IN:
                writer
                        .append(" in ($")
                        .append(parameterName)
                        .append(")");
                break;
            case ALL_IN:
                writer
                        .append(" ALL IN ($")
                        .append(parameterName)
                        .append(")");
                break;
            case BETWEEN:
                writer
                        .append(" BETWEEN $")
                        .append(fromParameterName)
                        .append(" AND $")
                        .append(toParameterName);
                break;
            case EQUALS:
                writer
                        .append(" = $")
                        .append(parameterName);
                break;

            case NOT_EQUALS:
                writer
                        .append(" != $")
                        .append(parameterName);
                break;
            case GREATER_THAN:
                writer
                        .append(" > $")
                        .append(parameterName);
                break;
            case GREATER_THAN_OR_EQUAL:
                writer
                        .append(" >= $")
                        .append(parameterName);
                break;
            case LESS_THAN:
                writer
                        .append(" < $")
                        .append(parameterName);
                break;
            case LESS_THAN_OR_EQUAL:
                writer
                        .append(" <= $")
                        .append(parameterName);
                break;
            case SEARCH:
                writer
                        .append(", $")
                        .append(parameterName);
                if (searchOperator == SearchOperator.AND) {
                    writer.append(", AND");
                }
                writer.append(")");
                break;
            case CMP_X_CHG_MATCH:
                writer
                        .append(", $")
                        .append(parameterName)
                        .append(")");
                break;
            case LUCENE:
            case STARTS_WITH:
            case ENDS_WITH:
            case REGEX:
                writer
                        .append(", $")
                        .append(parameterName)
                        .append(")");
                break;
            case EXISTS:
                writer
                        .append(")");
                break;
            case WITHIN:
            case CONTAINS:
            case DISJOINT:
            case INTERSECTS:
                writer
                        .append(", ");
                whereShape.writeTo(writer);

                if (Math.abs(distanceErrorPct - Constants.Documents.Indexing.Spatial.DEFAULT_DISTANCE_ERROR_PCT) > 1e-40) {
                    writer.append(", ");
                    writer.append(distanceErrorPct);
                }
                writer
                        .append(")");
                break;
            default:
                throw new IllegalArgumentException();
        }

        if (exact) {
            writer.append(")");
        }

        if (proximity != null) {
            writer
                    .append(", ")
                    .append(proximity)
                    .append(")");
        }

        if (fuzzy != null) {
            writer
                    .append(", ")
                    .append(fuzzy)
                    .append(")");
        }

        if (boost != null) {
            writer
                    .append(", ")
                    .append(boost)
                    .append(")");
        }
    }
}
