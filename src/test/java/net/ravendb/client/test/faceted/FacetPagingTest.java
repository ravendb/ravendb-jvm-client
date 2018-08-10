package net.ravendb.client.test.faceted;

import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.indexes.IndexDefinition;
import net.ravendb.client.documents.operations.indexes.PutIndexesOperation;
import net.ravendb.client.documents.queries.Query;
import net.ravendb.client.documents.queries.facets.*;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.test.FacetTestBase;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

public class FacetPagingTest extends FacetTestBase {

    private final List<Camera> _data;
    private final static int numCameras = 1000;

    public FacetPagingTest() {
        _data = getCameras(numCameras);
    }

    @Test
    public void canPerformFacetedPagingSearchWithNoPageSizeNoMaxResults_HitsDesc() throws Exception {
        FacetOptions facetOptions = new FacetOptions();
        facetOptions.setStart(2);
        facetOptions.setTermSortMode(FacetTermSortMode.COUNT_DESC);
        facetOptions.setIncludeRemainingTerms(true);

        Facet facet = new Facet();
        facet.setFieldName("manufacturer");
        facet.setOptions(facetOptions);

        List<Facet> facets = Collections.singletonList(facet);

        try (IDocumentStore store = getDocumentStore()) {
            setup(store);

            try (IDocumentSession session = store.openSession()) {
                FacetSetup facetSetup = new FacetSetup();
                facetSetup.setId("facets/CameraFacets");
                facetSetup.setFacets(facets);
                session.store(facetSetup);
                session.saveChanges();

                Map<String, FacetResult> facetResults = session.query(Camera.class, Query.index("CameraCost"))
                        .aggregateUsing("facets/CameraFacets")
                        .execute();

                Map<String, Long> cameraCounts = _data.stream()
                        .collect(Collectors.groupingBy(x -> x.getManufacturer(), Collectors.counting()));

                List<String> camerasByHits = cameraCounts
                        .entrySet()
                        .stream()
                        .sorted(Comparator.<Map.Entry<String, Long>>comparingLong(x -> x.getValue()).reversed().thenComparing(x -> x.getKey()))
                        .skip(2)
                        .map(x -> x.getKey().toLowerCase())
                        .collect(Collectors.toList());

                assertThat(facetResults.get("manufacturer").getValues())
                        .hasSize(3);

                assertThat(facetResults.get("manufacturer").getValues().get(0).getRange())
                        .isEqualTo(camerasByHits.get(0));
                assertThat(facetResults.get("manufacturer").getValues().get(1).getRange())
                        .isEqualTo(camerasByHits.get(1));
                assertThat(facetResults.get("manufacturer").getValues().get(2).getRange())
                        .isEqualTo(camerasByHits.get(2));

                for (FacetValue f : facetResults.get("manufacturer").getValues()) {

                    long inMemoryCount = _data.stream().filter(x -> x.getManufacturer().toLowerCase().equalsIgnoreCase(f.getRange())).count();
                    assertThat(f.getCount())
                            .isEqualTo((int)inMemoryCount);

                    assertThat(facetResults.get("manufacturer").getRemainingTermsCount())
                            .isEqualTo(0);
                    assertThat(facetResults.get("manufacturer").getRemainingTerms().size())
                            .isEqualTo(0);
                    assertThat(facetResults.get("manufacturer").getRemainingHits())
                            .isEqualTo(0);

                }
            }
        }
    }

    @Test
    public void canPerformFacetedPagingSearchWithNoPageSizeWithMaxResults_HitsDesc() throws Exception {
        FacetOptions facetOptions = new FacetOptions();
        facetOptions.setStart(2);
        facetOptions.setPageSize(2);
        facetOptions.setTermSortMode(FacetTermSortMode.COUNT_DESC);
        facetOptions.setIncludeRemainingTerms(true);

        Facet facet = new Facet();
        facet.setFieldName("manufacturer");
        facet.setOptions(facetOptions);

        List<Facet> facets = Arrays.asList(facet);

        try (IDocumentStore store = getDocumentStore()) {
            setup(store);

            try (IDocumentSession session = store.openSession()) {
                FacetSetup facetSetup = new FacetSetup();
                facetSetup.setId("facets/CameraFacets");
                facetSetup.setFacets(facets);
                session.store(facetSetup);
                session.saveChanges();

                Map<String, FacetResult> facetResults = session.query(Camera.class, Query.index("CameraCost"))
                        .aggregateUsing("facets/CameraFacets")
                        .execute();

                Map<String, Long> cameraCounts = _data.stream()
                        .collect(Collectors.groupingBy(x -> x.getManufacturer(), Collectors.counting()));

                List<String> camerasByHits = cameraCounts
                        .entrySet()
                        .stream()
                        .sorted(Comparator.<Map.Entry<String, Long>>comparingLong(x -> x.getValue()).reversed().thenComparing(x -> x.getKey()))
                        .skip(2)
                        .limit(2)
                        .map(x -> x.getKey().toLowerCase())
                        .collect(Collectors.toList());

                assertThat(facetResults.get("manufacturer").getValues())
                        .hasSize(2);

                assertThat(facetResults.get("manufacturer").getValues().get(0).getRange())
                        .isEqualTo(camerasByHits.get(0));
                assertThat(facetResults.get("manufacturer").getValues().get(1).getRange())
                        .isEqualTo(camerasByHits.get(1));

                for (FacetValue f : facetResults.get("manufacturer").getValues()) {

                    long inMemoryCount = _data.stream().filter(x -> x.getManufacturer().toLowerCase().equalsIgnoreCase(f.getRange())).count();
                    assertThat(f.getCount())
                            .isEqualTo((int)inMemoryCount);

                    assertThat(facetResults.get("manufacturer").getRemainingTermsCount())
                            .isEqualTo(1);
                    assertThat(facetResults.get("manufacturer").getRemainingTerms().size())
                            .isEqualTo(1);

                    List<Long> counts = cameraCounts
                            .entrySet()
                            .stream()
                            .sorted(Comparator.<Map.Entry<String, Long>>comparingLong(x -> x.getValue()).reversed().thenComparing(x -> x.getKey()))
                            .map(x -> x.getValue())
                            .collect(Collectors.toList());

                    assertThat(counts.get(counts.size() - 1))
                            .isEqualTo(facetResults.get("manufacturer").getRemainingHits());
                }
            }
        }
    }

    private void setup(IDocumentStore store) {
        try (IDocumentSession s = store.openSession()) {

            IndexDefinition indexDefinition = new IndexDefinition();
            indexDefinition.setName("CameraCost");
            indexDefinition.setMaps(Collections.singleton("from camera in docs select new { camera.manufacturer, camera.model, camera.cost, camera.dateOfListing, camera.megapixels } "));

            store.maintenance().send(new PutIndexesOperation(indexDefinition));

            int counter = 0;
            for (Camera camera : _data) {
                s.store(camera);
                counter++;

                if (counter % (numCameras / 25) == 0) {
                    s.saveChanges();
                }
            }

            s.saveChanges();
        }

        waitForIndexing(store);
    }
}
