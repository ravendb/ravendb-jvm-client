package net.ravendb.client;

import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.operations.revisions.ConfigureRevisionsOperation;
import net.ravendb.client.documents.operations.revisions.RevisionsCollectionConfiguration;
import net.ravendb.client.documents.operations.revisions.RevisionsConfiguration;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.http.RequestExecutor;
import net.ravendb.client.infrastructure.graph.*;
import net.ravendb.client.primitives.CleanCloseable;
import net.ravendb.client.driver.RavenServerLocator;
import net.ravendb.client.driver.RavenTestDriver;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHost;
import org.apache.http.client.config.RequestConfig;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;

@SuppressWarnings("SameParameterValue")
public class RemoteTestBase extends RavenTestDriver {

    private static class TestServiceLocator extends RavenServerLocator {

        @Override
        public String[] getCommandArguments() {
            return new String[] { "--ServerUrl=http://127.0.0.1:0", "--ServerUrl.Tcp=tcp://127.0.0.1:38881", "--Features.Availability=Experimental" };
        }
    }

    private static class TestSecuredServiceLocator extends RavenServerLocator {

        public static final String ENV_CERTIFICATE_PATH = "RAVENDB_JAVA_TEST_CERTIFICATE_PATH";

        public static final String ENV_TEST_CA_PATH = "RAVENDB_JAVA_TEST_CA_PATH";

        public static final String ENV_HTTPS_SERVER_URL = "RAVENDB_JAVA_TEST_HTTPS_SERVER_URL";

        @Override
        public String[] getCommandArguments() {
            String httpsServerUrl = getHttpsServerUrl();

            try {
                URL url = new URL(httpsServerUrl);
                String host = url.getHost();
                String tcpServerUrl = "tcp://" + host + ":38882";

                return new String[]{
                        "--Security.Certificate.Path=" + getServerCertificatePath(),
                        "--ServerUrl=" + httpsServerUrl,
                        "--ServerUrl.Tcp=" + tcpServerUrl
                };
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            }
        }

        private String getHttpsServerUrl() {
            String httpsServerUrl = System.getenv(ENV_HTTPS_SERVER_URL);
            if (StringUtils.isBlank(httpsServerUrl)) {
                throw new IllegalStateException("Unable to find RavenDB https server url. " +
                        "Please make sure " + ENV_HTTPS_SERVER_URL + " environment variable is set and is valid " +
                        "(current value = " + httpsServerUrl + ")");
            }

            return httpsServerUrl;
        }

        @Override
        public String getServerCertificatePath() {
            String certificatePath = System.getenv(ENV_CERTIFICATE_PATH);
            if (StringUtils.isBlank(certificatePath)) {
                throw new IllegalStateException("Unable to find RavenDB server certificate path. " +
                        "Please make sure " + ENV_CERTIFICATE_PATH + " environment variable is set and is valid " +
                        "(current value = " + certificatePath + ")");
            }

            return certificatePath;
        }


        @Override
        public String getServerCaPath() {
            return System.getenv(ENV_TEST_CA_PATH);
        }
    }

    public RemoteTestBase() {
        super(new TestServiceLocator(), new TestSecuredServiceLocator());
    }


    public CleanCloseable withFiddler() {
        RequestExecutor.requestPostProcessor = request -> {
            HttpHost proxy = new HttpHost("127.0.0.1", 8888, "http");
            RequestConfig requestConfig = request.getConfig();
            if (requestConfig == null) {
                requestConfig = RequestConfig.DEFAULT;
            }
            requestConfig = RequestConfig.copy(requestConfig).setProxy(proxy).build();
            request.setConfig(requestConfig);
        };

        return () -> RequestExecutor.requestPostProcessor = null;
    }

    @SuppressWarnings("UnusedReturnValue")
    protected ConfigureRevisionsOperation.ConfigureRevisionsOperationResult setupRevisions(IDocumentStore store, boolean purgeOnDelete, long minimumRevisionsToKeep) {
        RevisionsConfiguration revisionsConfiguration = new RevisionsConfiguration();
        RevisionsCollectionConfiguration defaultCollection = new RevisionsCollectionConfiguration();
        defaultCollection.setPurgeOnDelete(purgeOnDelete);
        defaultCollection.setMinimumRevisionsToKeep(minimumRevisionsToKeep);

        revisionsConfiguration.setDefaultConfig(defaultCollection);
        ConfigureRevisionsOperation operation = new ConfigureRevisionsOperation(revisionsConfiguration);

        return store.maintenance().send(operation);
    }

    protected void createSimpleData(IDocumentStore store) {
        try (IDocumentSession session = store.openSession()) {
            Entity entityA = new Entity();
            entityA.setId("entity/1");
            entityA.setName("A");

            Entity entityB = new Entity();
            entityB.setId("entity/2");
            entityB.setName("B");

            Entity entityC = new Entity();
            entityC.setId("entity/3");
            entityC.setName("C");

            session.store(entityA);
            session.store(entityB);
            session.store(entityC);

            entityA.setReferences(entityB.getId());
            entityB.setReferences(entityC.getId());
            entityC.setReferences(entityA.getId());

            session.saveChanges();
        }
    }

    protected void createDogDataWithoutEdges(IDocumentStore store) {
        try (IDocumentSession session = store.openSession()) {
            Dog arava = new Dog();
            arava.setName("Arava");

            Dog oscar = new Dog();
            oscar.setName("Oscar");

            Dog pheobe = new Dog();
            pheobe.setName("Pheobe");

            session.store(arava);
            session.store(oscar);
            session.store(pheobe);

            session.saveChanges();
        }
    }

    protected void createDataWithMultipleEdgesOfTheSameType(IDocumentStore store) {
        try (IDocumentSession session = store.openSession()) {
            Dog arava = new Dog();
            arava.setName("Arava");

            Dog oscar = new Dog();
            oscar.setName("Oscar");

            Dog pheobe = new Dog();
            pheobe.setName("Pheobe");

            session.store(arava);
            session.store(oscar);
            session.store(pheobe);

            //dogs/1 => dogs/2
            arava.setLikes(new String[] { oscar.getId() });
            arava.setDislikes(new String[] { pheobe.getId() });

            //dogs/2 => dogs/2,dogs/3 (cycle!)
            oscar.setLikes(new String[] { oscar.getId(), pheobe.getId() });
            oscar.setDislikes(new String[0]);

            //dogs/3 => dogs/2
            pheobe.setLikes(new String[] { oscar.getId() });
            pheobe.setDislikes(new String[] { arava.getId() });

            session.saveChanges();
        }
    }

    protected void createMoviesData(IDocumentStore store) {
        try (IDocumentSession session = store.openSession()) {
            Genre scifi = new Genre();
            scifi.setName("genres/1");
            scifi.setId("genres/1");

            Genre fantasy = new Genre();
            fantasy.setId("genres/2");
            fantasy.setName("Fantasy");

            Genre adventure = new Genre();
            adventure.setId("genres/3");
            adventure.setName("Adventure");

            session.store(scifi);
            session.store(fantasy);
            session.store(adventure);

            Movie starwars = new Movie();
            starwars.setId("movies/1");
            starwars.setName("Star Wars Ep.1");
            starwars.setGenres(Arrays.asList("genres/1", "genres/2"));

            Movie firefly = new Movie();
            firefly.setId("movies/2");
            firefly.setName("Firefly Serenity");
            firefly.setGenres(Arrays.asList("genres/2", "genres/3"));

            Movie indianaJones = new Movie();
            indianaJones.setId("movies/3");
            indianaJones.setName("Indiana Jones and the Temple Of Doom");
            indianaJones.setGenres(Arrays.asList("genres/3"));

            session.store(starwars);
            session.store(firefly);
            session.store(indianaJones);

            User user1 = new User();
            user1.setId("users/1");
            user1.setName("Jack");

            User.Rating rating11 = new User.Rating();
            rating11.setMovie("movies/1");
            rating11.setScore(5);

            User.Rating rating12 = new User.Rating();
            rating12.setMovie("movies/2");
            rating12.setScore(7);

            user1.setHasRated(Arrays.asList(rating11, rating12));
            session.store(user1);

            User user2 = new User();
            user2.setId("users/2");
            user2.setName("Jill");

            User.Rating rating21 = new User.Rating();
            rating21.setMovie("movies/2");
            rating21.setScore(7);

            User.Rating rating22 = new User.Rating();
            rating22.setMovie("movies/3");
            rating22.setScore(9);

            user2.setHasRated(Arrays.asList(rating11, rating22));

            session.store(user2);

            User user3 = new User();
            user3.setId("users/3");
            user3.setName("Bob");

            User.Rating rating31 = new User.Rating();
            rating31.setMovie("movies/3");
            rating31.setScore(5);

            user3.setHasRated(Arrays.asList(rating31));

            session.store(user3);

            session.saveChanges();
        }
    }
}
