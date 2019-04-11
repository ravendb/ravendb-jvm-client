package net.ravendb.client.documents.queries;

import net.ravendb.client.documents.session.tokens.DeclareToken;
import net.ravendb.client.documents.session.tokens.LoadToken;

import java.util.List;

public class QueryData {

    private String[] fields;
    private String[] projections;
    private String fromAlias;
    private DeclareToken declareToken;
    private List<LoadToken> loadTokens;
    private boolean isCustomFunction;
    private boolean mapReduce;

    public boolean isMapReduce() {
        return mapReduce;
    }

    public void setMapReduce(boolean mapReduce) {
        this.mapReduce = mapReduce;
    }

    public String[] getFields() {
        return fields;
    }

    public void setFields(String[] fields) {
        this.fields = fields;
    }

    public String[] getProjections() {
        return projections;
    }

    public void setProjections(String[] projections) {
        this.projections = projections;
    }

    public String getFromAlias() {
        return fromAlias;
    }

    public void setFromAlias(String fromAlias) {
        this.fromAlias = fromAlias;
    }

    public DeclareToken getDeclareToken() {
        return declareToken;
    }

    public void setDeclareToken(DeclareToken declareToken) {
        this.declareToken = declareToken;
    }

    public List<LoadToken> getLoadTokens() {
        return loadTokens;
    }

    public void setLoadTokens(List<LoadToken> loadTokens) {
        this.loadTokens = loadTokens;
    }

    public boolean isCustomFunction() {
        return isCustomFunction;
    }

    public void setCustomFunction(boolean customFunction) {
        isCustomFunction = customFunction;
    }


    public QueryData(String[] fields, String[] projections) {
        this(fields, projections, null, null, null, false);
    }

    public QueryData(String[] fields, String[] projections, String fromAlias, DeclareToken declareToken, List<LoadToken> loadTokens, boolean isCustomFunction) {
        this.fields = fields;
        this.projections = projections;
        this.fromAlias = fromAlias;
        this.declareToken = declareToken;
        this.loadTokens = loadTokens;
        this.isCustomFunction = isCustomFunction;
    }

    public static QueryData customFunction(String alias, String func) {
        return new QueryData(new String[]{ func }, new String[0], alias, null, null, true);
    }

}
