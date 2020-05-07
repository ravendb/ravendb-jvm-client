package net.ravendb.client.documents.queries;

import net.ravendb.client.documents.session.tokens.DeclareToken;
import net.ravendb.client.documents.session.tokens.LoadToken;

import java.util.List;

public class QueryData {

    private String[] fields;
    private String[] projections;
    private String fromAlias;
    private List<DeclareToken> declareTokens;
    private List<LoadToken> loadTokens;
    private boolean isCustomFunction;
    private boolean mapReduce;
    private boolean isProjectInto;

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

    public List<DeclareToken> getDeclareTokens() {
        return declareTokens;
    }

    public void setDeclareTokens(List<DeclareToken> declareTokens) {
        this.declareTokens = declareTokens;
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

    public QueryData(String[] fields, String[] projections, String fromAlias, List<DeclareToken> declareTokens, List<LoadToken> loadTokens, boolean isCustomFunction) {
        this.fields = fields;
        this.projections = projections;
        this.fromAlias = fromAlias;
        this.declareTokens = declareTokens;
        this.loadTokens = loadTokens;
        this.isCustomFunction = isCustomFunction;
    }

    public static QueryData customFunction(String alias, String func) {
        return new QueryData(new String[]{ func }, new String[0], alias, null, null, true);
    }

    public boolean isProjectInto() {
        return isProjectInto;
    }

    public void setProjectInto(boolean projectInto) {
        isProjectInto = projectInto;
    }
}
