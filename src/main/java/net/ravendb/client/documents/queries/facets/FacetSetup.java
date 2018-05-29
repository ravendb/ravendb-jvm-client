package net.ravendb.client.documents.queries.facets;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

public class FacetSetup {

    private String id;

    @JsonProperty("Facets")
    private List<Facet> facets;

    @JsonProperty("RangeFacets")
    private List<RangeFacet> rangeFacets;

    public FacetSetup() {
        facets = new ArrayList<>();
        rangeFacets = new ArrayList<>();
    }


    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public List<Facet> getFacets() {
        return facets;
    }

    public void setFacets(List<Facet> facets) {
        this.facets = facets;
    }

    public List<RangeFacet> getRangeFacets() {
        return rangeFacets;
    }

    public void setRangeFacets(List<RangeFacet> rangeFacets) {
        this.rangeFacets = rangeFacets;
    }
}
