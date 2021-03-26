package net.ravendb.client.driver;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Stopwatch;
import net.ravendb.client.documents.DocumentStore;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.http.RequestExecutor;
import net.ravendb.client.infrastructure.graph.*;
import net.ravendb.client.primitives.CleanCloseable;
import net.ravendb.client.primitives.Reference;
import net.ravendb.client.util.UrlUtils;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpHost;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;

import java.awt.*;
import java.io.*;
import java.lang.reflect.Field;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public abstract class RavenTestDriver {

    protected boolean disposed;

    public boolean isDisposed() {
        return disposed;
    }

    public static boolean debug;



    protected static void reportError(Exception e) {
        if (!debug) {
            return;
        }

        if (e == null) {
            throw new IllegalArgumentException("Exception can not be null");
        }
    }

    @SuppressWarnings("EmptyMethod")
    protected static void reportInfo(String message) {
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

    @SuppressWarnings("EmptyMethod")
    protected void setupDatabase(IDocumentStore documentStore) {
        // empty by design
    }

    protected IDocumentStore runServerInternal(RavenServerLocator locator, Reference<Process> processReference, Consumer<DocumentStore> configureStore) throws Exception {
        Process process = RavenServerRunner.run(locator);
        processReference.value = process;

        reportInfo("Starting global server");

        String url = null;
        InputStream stdout = process.getInputStream();

        Stopwatch startupDuration = Stopwatch.createStarted();
        BufferedReader reader = new BufferedReader(new InputStreamReader(stdout));

        List<String> readLines = new ArrayList<>();

        while (true) {
            String line = reader.readLine();
            readLines.add(line);

            if (line == null) {
                throw new RuntimeException(readLines
                        .stream()
                        .collect(Collectors.joining(System.lineSeparator())) + IOUtils.toString(process.getInputStream(), StandardCharsets.UTF_8));
            }

            if (startupDuration.elapsed(TimeUnit.MINUTES) >= 1) {
                break;
            }

            String prefix = "Server available on: ";
            if (line.startsWith(prefix)) {
                url = line.substring(prefix.length());
                break;
            }
        }

        if (url == null) {
            reportInfo("Url is null");

            try {
                process.destroyForcibly();
            } catch (Exception e) {
                reportError(e);
            }

            throw new IllegalStateException("Unable to start server");
        }

        DocumentStore store = new DocumentStore();
        store.setUrls(new String[] { url });
        store.setDatabase("test.manager");
        store.getConventions().setDisableTopologyUpdates(true);

        if (configureStore != null) {
            configureStore.accept(store);
        }

        return store.initialize();
    }

    protected <T> T waitForValue(Supplier<T> act, T expectedValue) throws InterruptedException {
        return waitForValue(act, expectedValue, Duration.ofSeconds(15));
    }

    protected <T> T waitForValue(Supplier<T> act, T expectedValue, Duration timeout) throws InterruptedException {
        Stopwatch sw = Stopwatch.createStarted();

        do {
            try {
                T currentVal = act.get();
                if (expectedValue.equals(currentVal)) {
                    return currentVal;
                }

                if (sw.elapsed().compareTo(timeout) > 0) {
                    return currentVal;
                }
            } catch (Exception e) {
                if (sw.elapsed().compareTo(timeout) > 0) {
                    throw new RuntimeException(e);
                }
            }

            Thread.sleep(16);
        } while (true);
    }

    protected static void killProcess(Process p) {
        if (p != null && p.isAlive()) {
            reportInfo("Kill global server");

            try {
                p.destroyForcibly();
            } catch (Exception e) {
                reportError(e);
            }
        }
    }

    public void waitForUserToContinueTheTest(IDocumentStore store) {
        String databaseNameEncoded = UrlUtils.escapeDataString(store.getDatabase());
        String documentsPage = store.getUrls()[0] + "/studio/index.html#databases/documents?&database=" + databaseNameEncoded + "&withStop=true";

        openBrowser(documentsPage);

        do {
            try {
                Thread.sleep(500);
            } catch (InterruptedException ignored) {
            }

            try (IDocumentSession session = store.openSession()) {
                if (session.load(ObjectNode.class, "Debug/Done") != null) {
                    break;
                }
            }

        } while (true);
    }

    protected void openBrowser(String url) {
        System.out.println(url);

        if (Desktop.isDesktopSupported()) {
            Desktop desktop = Desktop.getDesktop();
            try {
                desktop.browse(new URI(url));
            } catch (IOException | URISyntaxException e) {
                throw new RuntimeException(e);
            }
        } else {
            Runtime runtime = Runtime.getRuntime();
            try {
                runtime.exec("xdg-open " + url);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }


    protected static void createSimpleData(IDocumentStore store) {
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

    protected static void createDogDataWithoutEdges(IDocumentStore store) {
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

    protected static void createDataWithMultipleEdgesOfTheSameType(IDocumentStore store) {
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

    protected static void createMoviesData(IDocumentStore store) {
        try (IDocumentSession session = store.openSession()) {
            Genre scifi = new Genre();
            scifi.setId("genres/1");
            scifi.setName("Sci-Fi");

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

            user2.setHasRated(Arrays.asList(rating21, rating22));

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
