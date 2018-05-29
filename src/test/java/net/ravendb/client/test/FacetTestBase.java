package net.ravendb.client.test;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.indexes.AbstractIndexCreationTask;
import net.ravendb.client.documents.indexes.IndexDefinition;
import net.ravendb.client.documents.operations.indexes.PutIndexesOperation;
import net.ravendb.client.documents.queries.facets.Facet;
import net.ravendb.client.documents.queries.facets.FacetBase;
import net.ravendb.client.documents.queries.facets.RangeFacet;
import net.ravendb.client.documents.session.IDocumentSession;

import java.util.*;

public abstract class FacetTestBase extends RemoteTestBase {

    public static void createCameraCostIndex(IDocumentStore store) {
        CameraCostIndex index = new CameraCostIndex();
        store.maintenance().send(new PutIndexesOperation(index.createIndexDefinition()));
    }

    public static class CameraCostIndex extends AbstractIndexCreationTask {
        @Override
        public IndexDefinition createIndexDefinition() {
            IndexDefinition indexDefinition = new IndexDefinition();
            indexDefinition.setMaps(Collections.singleton("from camera in docs.Cameras select new  { camera.manufacturer,\n" +
                    "                            camera.model,\n" +
                    "                            camera.cost,\n" +
                    "                            camera.dateOfListing,\n" +
                    "                            camera.megapixels" +
                    " }"));
            indexDefinition.setName("CameraCost");

            return indexDefinition;
        }

        @Override
        public String getIndexName() {
            return "CameraCost";
        }
    }

    protected static void insertCameraData(IDocumentStore store, List<Camera> cameras, boolean waitForIndexing) {
        try (IDocumentSession session = store.openSession()) {
            for (Camera camera : cameras) {
                session.store(camera);
            }

            session.saveChanges();
        }

        if (waitForIndexing) {
            FacetTestBase.waitForIndexing(store);
        }
    }

    public static List<FacetBase> getFacets() {

        Facet facet1 = new Facet();
        facet1.setFieldName("manufacturer");

        RangeFacet costRangeFacet = new RangeFacet();
        costRangeFacet.setRanges(Arrays.asList(
                "cost <= 200",
                "cost >= 200 and cost <= 400",
                "cost >= 400 and cost <= 600",
                "cost >= 600 and cost <= 800",
                "cost >= 800"
        ));
        RangeFacet megaPixelsRangeFacet = new RangeFacet();
        megaPixelsRangeFacet.setRanges(Arrays.asList(
                "megapixels <= 3",
                "megapixels >= 3 and megapixels <= 7",
                "megapixels >= 7 and megapixels <= 10",
                "megapixels >= 10"
        ));

        return Arrays.asList(facet1, costRangeFacet, megaPixelsRangeFacet);
    }

    private static final List<String> FEATURES = Arrays.asList("Image Stabilizer", "Tripod", "Low Light Compatible", "Fixed Lens", "LCD");

    private static final List<String> MANUFACTURERS = Arrays.asList("Sony", "Nikon", "Phillips", "Canon", "Jessops");

    private static final List<String> MODELS = Arrays.asList("Model1", "Model2", "Model3", "Model4", "Model5");

    private static final Random RANDOM = new Random(1337);

    @SuppressWarnings({"deprecation", "SameParameterValue"})
    protected static List<Camera> getCameras(int numCameras) {
        ArrayList<Camera> cameraList = new ArrayList<>(numCameras);
        for (int i = 1; i <= numCameras; i++) {
            Camera camera = new Camera();
            camera.setDateOfListing(new Date(80 + RANDOM.nextInt(30), RANDOM.nextInt(12), RANDOM.nextInt(27)));
            camera.setManufacturer(MANUFACTURERS.get(RANDOM.nextInt(MANUFACTURERS.size())));
            camera.setModel(MODELS.get(RANDOM.nextInt(MODELS.size())));
            camera.setCost(RANDOM.nextDouble() * 900 + 100);
            camera.setZoom((int)(RANDOM.nextDouble() * 10 + 1.0));
            camera.setMegapixels(RANDOM.nextDouble() * 10 + 1.0);
            camera.setImageStabilizer(RANDOM.nextDouble() > 0.6);
            camera.setAdvancedFeatures(Arrays.asList("??"));

            cameraList.add(camera);
        }

        return cameraList;
    }

    public static class Camera {
        private String id;

        private Date dateOfListing;
        private String manufacturer;
        private String model;
        private double cost;

        private int zoom;
        private double megapixels;
        private boolean imageStabilizer;
        private List<String> advancedFeatures;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public Date getDateOfListing() {
            return dateOfListing;
        }

        public void setDateOfListing(Date dateOfListing) {
            this.dateOfListing = dateOfListing;
        }

        public String getManufacturer() {
            return manufacturer;
        }

        public void setManufacturer(String manufacturer) {
            this.manufacturer = manufacturer;
        }

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }

        public double getCost() {
            return cost;
        }

        public void setCost(double cost) {
            this.cost = cost;
        }

        public int getZoom() {
            return zoom;
        }

        public void setZoom(int zoom) {
            this.zoom = zoom;
        }

        public double getMegapixels() {
            return megapixels;
        }

        public void setMegapixels(double megapixels) {
            this.megapixels = megapixels;
        }

        public boolean isImageStabilizer() {
            return imageStabilizer;
        }

        public void setImageStabilizer(boolean imageStabilizer) {
            this.imageStabilizer = imageStabilizer;
        }

        public List<String> getAdvancedFeatures() {
            return advancedFeatures;
        }

        public void setAdvancedFeatures(List<String> advancedFeatures) {
            this.advancedFeatures = advancedFeatures;
        }
    }
}
