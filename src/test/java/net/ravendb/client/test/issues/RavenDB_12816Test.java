package net.ravendb.client.test.issues;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.queries.facets.Facet;
import net.ravendb.client.documents.queries.facets.FacetResult;
import net.ravendb.client.documents.queries.facets.FacetSetup;
import net.ravendb.client.documents.queries.facets.RangeFacet;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.test.FacetTestBase;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class RavenDB_12816Test extends RemoteTestBase {

    @Test
    public void canSendFacetedRawQuery() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            FacetTestBase.CameraCostIndex index = new FacetTestBase.CameraCostIndex();
            index.execute(store);

            try (IDocumentSession session = store.openSession()) {
                for (int i = 0; i < 10; i++) {
                    FacetTestBase.Camera camera = new FacetTestBase.Camera();
                    camera.setId("cameras/" + i);
                    camera.setManufacturer(i % 2 == 0 ? "Manufacturer1" : "Manufacturer2");
                    camera.setCost(i * 100);
                    camera.setMegapixels(i);
                    session.store(camera);
                }
                session.saveChanges();
            }

            waitForIndexing(store);

            List<Facet> facets = new ArrayList<>();

            Facet facet1 = new Facet();
            facet1.setFieldName("manufacturer");

            facets.add(facet1);

            List<RangeFacet> rangeFacets = new ArrayList<>();

            RangeFacet rangeFacet1 = new RangeFacet();
            rangeFacet1.setRanges(Arrays.asList(
                    "cost <= 200",
                    "cost >= 300 and cost <= 400",
                    "cost >= 500 and cost <= 600",
                    "cost >= 700 and cost <= 800",
                    "cost >= 900"));

            RangeFacet rangeFacet2 = new RangeFacet();
            rangeFacet2
                    .setRanges(Arrays.asList("megapixels <= 3",
                            "megapixels >= 4 and megapixels <= 7",
                            "megapixels >= 8 and megapixels <= 10",
                            "megapixels >= 11"));

            rangeFacets.add(rangeFacet1);
            rangeFacets.add(rangeFacet2);

            try (IDocumentSession session = store.openSession()) {
                FacetSetup facetSetup = new FacetSetup();
                facetSetup.setId("facets/CameraFacets");
                facetSetup.setFacets(facets);
                facetSetup.setRangeFacets(rangeFacets);
                session.store(facetSetup);
                session.saveChanges();
            }

            waitForIndexing(store);

            try (IDocumentSession session = store.openSession()) {
                Map<String, FacetResult> facetResults = session
                        .advanced()
                        .rawQuery(FacetTestBase.Camera.class, "from index 'CameraCost' select facet(id('facets/CameraFacets'))")
                        .executeAggregation();

                assertThat(facetResults)
                        .hasSize(3);

                assertThat(facetResults.get("manufacturer").getValues())
                        .hasSize(2);
                assertThat(facetResults.get("manufacturer").getValues().get(0).getRange())
                        .isEqualTo("manufacturer1");
                assertThat(facetResults.get("manufacturer").getValues().get(0).getCount())
                        .isEqualTo(5);
                assertThat(facetResults.get("manufacturer").getValues().get(1).getRange())
                        .isEqualTo("manufacturer2");
                assertThat(facetResults.get("manufacturer").getValues().get(1).getCount())
                        .isEqualTo(5);

                assertThat(facetResults.get("cost").getValues())
                        .hasSize(5);

                assertThat(facetResults.get("cost").getValues().get(0).getRange())
                        .isEqualTo("cost <= 200");
                assertThat(facetResults.get("cost").getValues().get(0).getCount())
                        .isEqualTo(3);
                assertThat(facetResults.get("cost").getValues().get(1).getRange())
                        .isEqualTo("cost >= 300 and cost <= 400");
                assertThat(facetResults.get("cost").getValues().get(1).getCount())
                        .isEqualTo(2);
                assertThat(facetResults.get("cost").getValues().get(2).getRange())
                        .isEqualTo("cost >= 500 and cost <= 600");
                assertThat(facetResults.get("cost").getValues().get(2).getCount())
                        .isEqualTo(2);
                assertThat(facetResults.get("cost").getValues().get(3).getRange())
                        .isEqualTo("cost >= 700 and cost <= 800");
                assertThat(facetResults.get("cost").getValues().get(3).getCount())
                        .isEqualTo(2);
                assertThat(facetResults.get("cost").getValues().get(4).getRange())
                        .isEqualTo("cost >= 900");
                assertThat(facetResults.get("cost").getValues().get(4).getCount())
                        .isEqualTo(1);

                assertThat(facetResults.get("megapixels").getValues())
                        .hasSize(4);
                assertThat(facetResults.get("megapixels").getValues().get(0).getRange())
                        .isEqualTo("megapixels <= 3");
                assertThat(facetResults.get("megapixels").getValues().get(0).getCount())
                        .isEqualTo(4);
                assertThat(facetResults.get("megapixels").getValues().get(1).getRange())
                        .isEqualTo("megapixels >= 4 and megapixels <= 7");
                assertThat(facetResults.get("megapixels").getValues().get(1).getCount())
                        .isEqualTo(4);
                assertThat(facetResults.get("megapixels").getValues().get(2).getRange())
                        .isEqualTo("megapixels >= 8 and megapixels <= 10");
                assertThat(facetResults.get("megapixels").getValues().get(2).getCount())
                        .isEqualTo(2);
                assertThat(facetResults.get("megapixels").getValues().get(3).getRange())
                        .isEqualTo("megapixels >= 11");
                assertThat(facetResults.get("megapixels").getValues().get(3).getCount())
                        .isEqualTo(0);
            }

            try (IDocumentSession session = store.openSession()) {
                Map<String, FacetResult> r1 = session
                        .advanced()
                        .rawQuery(FacetTestBase.Camera.class, "from index 'CameraCost' where cost < 200 select facet(id('facets/CameraFacets'))")
                        .executeAggregation();

                Map<String, FacetResult> r2 = session
                        .advanced()
                        .rawQuery(FacetTestBase.Camera.class, "from index 'CameraCost' where megapixels < 3 select facet(id('facets/CameraFacets'))")
                        .executeAggregation();

                List<Map<String, FacetResult>> multiFacetResults = Arrays.asList(r1, r2);

                assertThat(multiFacetResults.get(0))
                        .hasSize(3);

                assertThat(multiFacetResults.get(0).get("manufacturer").getValues())
                        .hasSize(2);
                assertThat(multiFacetResults.get(0).get("manufacturer").getValues().get(0).getRange())
                        .isEqualTo("manufacturer1");
                assertThat(multiFacetResults.get(0).get("manufacturer").getValues().get(0).getCount())
                        .isEqualTo(1);
                assertThat(multiFacetResults.get(0).get("manufacturer").getValues().get(1).getRange())
                        .isEqualTo("manufacturer2");
                assertThat(multiFacetResults.get(0).get("manufacturer").getValues().get(1).getCount())
                        .isEqualTo(1);

                assertThat(multiFacetResults.get(0).get("cost").getValues())
                        .hasSize(5);

                assertThat(multiFacetResults.get(0).get("cost").getValues().get(0).getRange())
                        .isEqualTo("cost <= 200");
                assertThat(multiFacetResults.get(0).get("cost").getValues().get(0).getCount())
                        .isEqualTo(2);
                assertThat(multiFacetResults.get(0).get("cost").getValues().get(1).getRange())
                        .isEqualTo("cost >= 300 and cost <= 400");
                assertThat(multiFacetResults.get(0).get("cost").getValues().get(1).getCount())
                        .isEqualTo(0);
                assertThat(multiFacetResults.get(0).get("cost").getValues().get(2).getRange())
                        .isEqualTo("cost >= 500 and cost <= 600");
                assertThat(multiFacetResults.get(0).get("cost").getValues().get(2).getCount())
                        .isEqualTo(0);
                assertThat(multiFacetResults.get(0).get("cost").getValues().get(3).getRange())
                        .isEqualTo("cost >= 700 and cost <= 800");
                assertThat(multiFacetResults.get(0).get("cost").getValues().get(3).getCount())
                        .isEqualTo(0);
                assertThat(multiFacetResults.get(0).get("cost").getValues().get(4).getRange())
                        .isEqualTo("cost >= 900");
                assertThat(multiFacetResults.get(0).get("cost").getValues().get(4).getCount())
                        .isEqualTo(0);

                assertThat(multiFacetResults.get(0).get("megapixels").getValues())
                        .hasSize(4);
                assertThat(multiFacetResults.get(0).get("megapixels").getValues().get(0).getRange())
                        .isEqualTo("megapixels <= 3");
                assertThat(multiFacetResults.get(0).get("megapixels").getValues().get(0).getCount())
                        .isEqualTo(2);
                assertThat(multiFacetResults.get(0).get("megapixels").getValues().get(1).getRange())
                        .isEqualTo("megapixels >= 4 and megapixels <= 7");
                assertThat(multiFacetResults.get(0).get("megapixels").getValues().get(1).getCount())
                        .isEqualTo(0);
                assertThat(multiFacetResults.get(0).get("megapixels").getValues().get(2).getRange())
                        .isEqualTo("megapixels >= 8 and megapixels <= 10");
                assertThat(multiFacetResults.get(0).get("megapixels").getValues().get(2).getCount())
                        .isEqualTo(0);
                assertThat(multiFacetResults.get(0).get("megapixels").getValues().get(3).getRange())
                        .isEqualTo("megapixels >= 11");
                assertThat(multiFacetResults.get(0).get("megapixels").getValues().get(3).getCount())
                        .isEqualTo(0);


                assertThat(multiFacetResults.get(1))
                        .hasSize(3);

                assertThat(multiFacetResults.get(1).get("manufacturer").getValues())
                        .hasSize(2);
                assertThat(multiFacetResults.get(1).get("manufacturer").getValues().get(0).getRange())
                        .isEqualTo("manufacturer1");
                assertThat(multiFacetResults.get(1).get("manufacturer").getValues().get(0).getCount())
                        .isEqualTo(2);
                assertThat(multiFacetResults.get(1).get("manufacturer").getValues().get(1).getRange())
                        .isEqualTo("manufacturer2");
                assertThat(multiFacetResults.get(1).get("manufacturer").getValues().get(1).getCount())
                        .isEqualTo(1);

                assertThat(multiFacetResults.get(1).get("cost").getValues())
                        .hasSize(5);

                assertThat(multiFacetResults.get(1).get("cost").getValues().get(0).getRange())
                        .isEqualTo("cost <= 200");
                assertThat(multiFacetResults.get(1).get("cost").getValues().get(0).getCount())
                        .isEqualTo(3);
                assertThat(multiFacetResults.get(1).get("cost").getValues().get(1).getRange())
                        .isEqualTo("cost >= 300 and cost <= 400");
                assertThat(multiFacetResults.get(1).get("cost").getValues().get(1).getCount())
                        .isEqualTo(0);
                assertThat(multiFacetResults.get(1).get("cost").getValues().get(2).getRange())
                        .isEqualTo("cost >= 500 and cost <= 600");
                assertThat(multiFacetResults.get(1).get("cost").getValues().get(2).getCount())
                        .isEqualTo(0);
                assertThat(multiFacetResults.get(1).get("cost").getValues().get(3).getRange())
                        .isEqualTo("cost >= 700 and cost <= 800");
                assertThat(multiFacetResults.get(1).get("cost").getValues().get(3).getCount())
                        .isEqualTo(0);
                assertThat(multiFacetResults.get(1).get("cost").getValues().get(4).getRange())
                        .isEqualTo("cost >= 900");
                assertThat(multiFacetResults.get(1).get("cost").getValues().get(4).getCount())
                        .isEqualTo(0);

                assertThat(multiFacetResults.get(1).get("megapixels").getValues())
                        .hasSize(4);
                assertThat(multiFacetResults.get(1).get("megapixels").getValues().get(0).getRange())
                        .isEqualTo("megapixels <= 3");
                assertThat(multiFacetResults.get(1).get("megapixels").getValues().get(0).getCount())
                        .isEqualTo(3);
                assertThat(multiFacetResults.get(1).get("megapixels").getValues().get(1).getRange())
                        .isEqualTo("megapixels >= 4 and megapixels <= 7");
                assertThat(multiFacetResults.get(1).get("megapixels").getValues().get(1).getCount())
                        .isEqualTo(0);
                assertThat(multiFacetResults.get(1).get("megapixels").getValues().get(2).getRange())
                        .isEqualTo("megapixels >= 8 and megapixels <= 10");
                assertThat(multiFacetResults.get(1).get("megapixels").getValues().get(2).getCount())
                        .isEqualTo(0);
                assertThat(multiFacetResults.get(1).get("megapixels").getValues().get(3).getRange())
                        .isEqualTo("megapixels >= 11");
                assertThat(multiFacetResults.get(1).get("megapixels").getValues().get(3).getCount())
                        .isEqualTo(0);
            }
        }
    }

    @Test
    public void usingToListOnRawFacetQueryShouldThrow() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            FacetTestBase.CameraCostIndex index = new FacetTestBase.CameraCostIndex();
            index.execute(store);

            List<Facet> facets = new ArrayList<>();
            Facet facet1 = new Facet();
            facet1.setFieldName("manufacturer");

            facets.add(facet1);

            List<RangeFacet> rangeFacets = new ArrayList<>();
            RangeFacet rangeFacet1 = new RangeFacet();
            rangeFacet1.setRanges(Arrays.asList(
                    "cost <= 200",
                    "cost >= 300 and cost <= 400",
                    "cost >= 500 and cost <= 600",
                    "cost >= 700 and cost <= 800",
                    "cost >= 900"));

            RangeFacet rangeFacet2 = new RangeFacet();
            rangeFacet2
                    .setRanges(Arrays.asList("megapixels <= 3",
                            "megapixels >= 4 and megapixels <= 7",
                            "megapixels >= 8 and megapixels <= 10",
                            "megapixels >= 11"));

            rangeFacets.add(rangeFacet1);
            rangeFacets.add(rangeFacet2);

            try (IDocumentSession session = store.openSession()) {
                FacetSetup facetSetup = new FacetSetup();
                facetSetup.setId("facets/CameraFacets");
                facetSetup.setFacets(facets);
                facetSetup.setRangeFacets(rangeFacets);
                session.store(facetSetup);
                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                assertThatThrownBy(() -> {
                    session.advanced().rawQuery(FacetTestBase.Camera.class, "from index 'CameraCost' select facet(id('facets/CameraFacets'))")
                            .toList();
                }).hasMessageStartingWith("Raw query with aggregation by facet should be called by executeAggregation method");
            }
        }
    }
}
