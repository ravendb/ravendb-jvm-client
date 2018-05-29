package net.ravendb.client.documents.queries.facets;

import com.fasterxml.jackson.annotation.JsonProperty;
import net.ravendb.client.documents.session.tokens.FacetToken;

import java.util.function.Function;

public class Facet extends FacetBase {

    @JsonProperty("FieldName")
    private String fieldName;

    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    @Override
    public FacetToken toFacetToken(Function<Object, String> addQueryParameter) {
        return FacetToken.create(this, addQueryParameter);
    }
}
