package net.ravendb.client.documents.queries.facets;

import com.fasterxml.jackson.annotation.JsonProperty;
import net.ravendb.client.documents.session.tokens.FacetToken;

import java.util.function.Function;

public class Facet extends FacetBase {

    @JsonProperty("FieldName")
    private String fieldName;

    @JsonProperty("Options")
    private FacetOptions options;

    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }


    public FacetOptions getOptions() {
        return options;
    }

    public void setOptions(FacetOptions options) {
        this.options = options;
    }

    @Override
    public FacetToken toFacetToken(Function<Object, String> addQueryParameter) {
        return FacetToken.create(this, addQueryParameter);
    }
}
