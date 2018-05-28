package net.ravendb.client.test.client.moreLikeThis;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.Lazy;
import net.ravendb.client.documents.indexes.AbstractIndexCreationTask;
import net.ravendb.client.documents.indexes.FieldIndexing;
import net.ravendb.client.documents.indexes.FieldStorage;
import net.ravendb.client.documents.indexes.FieldTermVector;
import net.ravendb.client.documents.queries.moreLikeThis.MoreLikeThisOptions;
import net.ravendb.client.documents.queries.moreLikeThis.MoreLikeThisStopWords;
import net.ravendb.client.documents.session.IDocumentSession;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

public class MoreLikeThisTest extends RemoteTestBase {

    private static String getLorem(int numWords) {
        String theLorem = "Morbi nec purus eu libero interdum laoreet Nam metus quam posuere in elementum eget egestas eget justo Aenean orci ligula ullamcorper nec convallis non placerat nec lectus Quisque convallis porta suscipit Aliquam sollicitudin ligula sit amet libero cursus egestas Maecenas nec mauris neque at faucibus justo Fusce ut orci neque Nunc sodales pulvinar lobortis Praesent dui tellus fermentum sed faucibus nec faucibus non nibh Vestibulum adipiscing porta purus ut varius mi pulvinar eu Nam sagittis sodales hendrerit Vestibulum et tincidunt urna Fusce lacinia nisl at luctus lobortis lacus quam rhoncus risus a posuere nulla lorem at nisi Sed non erat nisl Cras in augue velit a mattis ante Etiam lorem dui elementum eget facilisis vitae viverra sit amet tortor Suspendisse potenti Nunc egestas accumsan justo viverra viverra Sed faucibus ullamcorper mauris ut pharetra ligula ornare eget Donec suscipit luctus rhoncus Pellentesque eget justo ac nunc tempus consequat Nullam fringilla egestas leo Praesent condimentum laoreet magna vitae luctus sem cursus sed Mauris massa purus suscipit ac malesuada a accumsan non neque Proin et libero vitae quam ultricies rhoncus Praesent urna neque molestie et suscipit vestibulum iaculis ac nulla Integer porta nulla vel leo ullamcorper eu rhoncus dui semper Donec dictum dui";

        String[] loremArray = theLorem.split(" ");

        StringBuilder outoupt = new StringBuilder();
        Random rnd = new Random();

        for (int i = 0; i < numWords; i++) {
            outoupt.append(loremArray[rnd.nextInt(loremArray.length - 1)]).append(" ");
        }
        return outoupt.toString();
    }

    private static List<Data> getDataList() {
        ArrayList<Data> items = new ArrayList<>();

        items.add(new Data("This is a test. Isn't it great? I hope I pass my test!"));
        items.add(new Data("I have a test tomorrow. I hate having a test"));
        items.add(new Data("Cake is great."));
        items.add(new Data("This document has the word test only once"));
        items.add(new Data("test"));
        items.add(new Data("test"));
        items.add(new Data("test"));
        items.add(new Data("test"));

        return items;
    }

    @Test
    public void canGetResultsUsingTermVectors() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            String id;
            try (IDocumentSession session = store.openSession()) {

                new DataIndex(true, false).execute(store);

                List<Data> list = getDataList();
                list.forEach(session::store);
                session.saveChanges();

                id = session.advanced().getDocumentId(list.get(0));
                waitForIndexing(store);
            }

            assertMoreLikeThisHasMatchesFor(Data.class, DataIndex.class, store, id);
        }
    }

    @Test
    public void canGetResultsUsingTermVectorsLazy() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            String id;
            try (IDocumentSession session = store.openSession()) {

                new DataIndex(true, false).execute(store);

                List<Data> list = getDataList();
                list.forEach(session::store);
                session.saveChanges();

                id = session.advanced().getDocumentId(list.get(0));
                waitForIndexing(store);
            }

            try (IDocumentSession session = store.openSession()) {
                MoreLikeThisOptions options = new MoreLikeThisOptions();
                options.setFields(new String[]{ "body" });
                Lazy<List<Data>> lazyLst = session.query(Data.class, DataIndex.class)
                        .moreLikeThis(f -> f.usingDocument(b -> b.whereEquals("id()", id)).withOptions(options))
                        .lazily();

                List<Data> list = lazyLst.getValue();

                assertThat(list)
                        .isNotEmpty();
            }
        }
    }

    @Test
    public void canGetResultsUsingTermVectorsWithDocumentQuery() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            String id;

            try (IDocumentSession session = store.openSession()) {
                new DataIndex(true, false).execute(store);

                List<Data> list = getDataList();
                list.forEach(session::store);
                session.saveChanges();

                id = session.advanced().getDocumentId(list.get(0));
                waitForIndexing(store);
            }

            try (IDocumentSession session = store.openSession()) {

                MoreLikeThisOptions options = new MoreLikeThisOptions();
                options.setFields(new String[]{ "body" });

                List<Data> list = session.query(Data.class, DataIndex.class)
                        .moreLikeThis(f -> f.usingDocument(x -> x.whereEquals("id()", id)).withOptions(options))
                        .toList();

                assertThat(list)
                        .isNotEmpty();
            }
        }
    }

    @Test
    public void canGetResultsUsingStorage() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            String id;

            try (IDocumentSession session = store.openSession()) {
                new DataIndex(false, true).execute(store);

                List<Data> list = getDataList();
                list.forEach(session::store);
                session.saveChanges();

                id = session.advanced().getDocumentId(list.get(0));
                waitForIndexing(store);
            }

            assertMoreLikeThisHasMatchesFor(Data.class, DataIndex.class, store, id);
        }
    }

    @Test
    public void canGetResultsUsingTermVectorsAndStorage() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            String id;

            try (IDocumentSession session = store.openSession()) {
                new DataIndex(true, true).execute(store);

                List<Data> list = getDataList();
                list.forEach(session::store);
                session.saveChanges();

                id = session.advanced().getDocumentId(list.get(0));
                waitForIndexing(store);
            }

            assertMoreLikeThisHasMatchesFor(Data.class, DataIndex.class, store, id);
        }
    }

    @Test
    public void test_With_Lots_Of_Random_Data() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            String key = "datas/1-A";

            try (IDocumentSession session = store.openSession()) {
                new DataIndex().execute(store);

                for (int i = 0; i < 100; i++) {
                    Data data = new Data();
                    data.setBody(getLorem(200));
                    session.store(data);
                }

                session.saveChanges();

                waitForIndexing(store);
            }

            assertMoreLikeThisHasMatchesFor(Data.class, DataIndex.class, store, key);
        }
    }

    @Test
    public void do_Not_Pass_FieldNames() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            String key = "datas/1-A";

            try (IDocumentSession session = store.openSession()) {
                new DataIndex().execute(store);

                for (int i = 0; i < 10; i++) {
                    Data data = new Data();
                    data.setBody("Body" + i);
                    data.setWhitespaceAnalyzerField("test test");
                    session.store(data);
                }

                session.saveChanges();
                waitForIndexing(store);
            }

            try (IDocumentSession session = store.openSession()) {
                List<Data> list = session.query(Data.class, DataIndex.class)
                        .moreLikeThis(f -> f.usingDocument(x -> x.whereEquals("id()", key)))
                        .toList();

                assertThat(list)
                        .isNotEmpty();
            }
        }
    }

    @Test
    public void each_Field_Should_Use_Correct_Analyzer() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            final String key1 = "datas/1-A";

            try (IDocumentSession session = store.openSession()) {
                new DataIndex().execute(store);

                for (int i = 0; i < 10; i++) {
                    Data data = new Data();
                    data.setWhitespaceAnalyzerField("bob@hotmail.com hotmail");
                    session.store(data);
                }

                session.saveChanges();
                waitForIndexing(store);
            }

            try (IDocumentSession session = store.openSession()) {
                MoreLikeThisOptions options = new MoreLikeThisOptions();
                options.setMinimumTermFrequency(2);
                options.setMinimumDocumentFrequency(5);

                List<Data> list = session.query(Data.class, DataIndex.class)
                        .moreLikeThis(f -> f.usingDocument(x -> x.whereEquals("id()", key1)).withOptions(options))
                        .toList();

                assertThat(list)
                        .isEmpty();
            }

            final String key2 = "datas/11-A";

            try (IDocumentSession session = store.openSession()) {
                new DataIndex().execute(store);

                for (int i = 0; i < 10; i++) {
                    Data data = new Data();
                    data.setWhitespaceAnalyzerField("bob@hotmail.com bob@hotmail.com");
                    session.store(data);
                }

                session.saveChanges();
                waitForIndexing(store);
            }

            try (IDocumentSession session = store.openSession()) {
                List<Data> list = session.query(Data.class, DataIndex.class)
                        .moreLikeThis(f -> f.usingDocument(x -> x.whereEquals("id()", key2)))
                        .toList();

                assertThat(list)
                        .isNotEmpty();
            }
        }
    }

    @Test
    public void can_Use_Min_Doc_Freq_Param() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            final String key = "datas/1-A";

            try (IDocumentSession session = store.openSession()) {
                new DataIndex().execute(store);

                Consumer<String> factory = text -> {
                    Data data = new Data();
                    data.setBody(text);
                    session.store(data);
                };

                factory.accept("This is a test. Isn't it great? I hope I pass my test!");
                factory.accept("I have a test tomorrow. I hate having a test");
                factory.accept("Cake is great.");
                factory.accept("This document has the word test only once");

                session.saveChanges();

                waitForIndexing(store);
            }

            try (IDocumentSession session = store.openSession()) {
                MoreLikeThisOptions options = new MoreLikeThisOptions();
                options.setFields(new String[]{ "body" });
                options.setMinimumDocumentFrequency(2);
                List<Data> list = session.query(Data.class, DataIndex.class)
                        .moreLikeThis(f -> f.usingDocument(x -> x.whereEquals("id()", key)).withOptions(options))
                        .toList();

                assertThat(list)
                        .isNotEmpty();
            }
        }
    }

    @Test
    public void can_Use_Boost_Param() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            String key = "datas/1-A";

            try (IDocumentSession session = store.openSession()) {
                new DataIndex().execute(store);

                Consumer<String> factory = text -> {
                    Data data = new Data();
                    data.setBody(text);
                    session.store(data);
                };

                factory.accept("This is a test. it is a great test. I hope I pass my great test!");
                factory.accept("Cake is great.");
                factory.accept("I have a test tomorrow.");

                session.saveChanges();

                waitForIndexing(store);
            }

            try (IDocumentSession session = store.openSession()) {
                MoreLikeThisOptions options = new MoreLikeThisOptions();

                options.setFields(new String[]{ "body" });
                options.setMinimumWordLength(3);
                options.setMinimumDocumentFrequency(1);
                options.setMinimumTermFrequency(2);
                options.setBoost(true);

                List<Data> list = session.query(Data.class, DataIndex.class)
                        .moreLikeThis(f -> f.usingDocument(x -> x.whereEquals("id()", key)).withOptions(options))
                        .toList();

                assertThat(list)
                        .isNotEmpty();

                assertThat(list.get(0).getBody())
                        .isEqualTo("I have a test tomorrow.");
            }
        }
    }

    @Test
    public void can_Use_Stop_Words() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            String key = "datas/1-A";

            new DataIndex().execute(store);

            try (IDocumentSession session = store.openSession()) {
                Consumer<String> factory = text -> {
                    Data data = new Data();
                    data.setBody(text);
                    session.store(data);
                };

                factory.accept("This is a test. Isn't it great? I hope I pass my test!");
                factory.accept("I should not hit this document. I hope");
                factory.accept("Cake is great.");
                factory.accept("This document has the word test only once");
                factory.accept("test");
                factory.accept("test");
                factory.accept("test");
                factory.accept("test");

                MoreLikeThisStopWords stopWords = new MoreLikeThisStopWords();
                stopWords.setId("Config/Stopwords");
                stopWords.setStopWords(Arrays.asList("I", "A", "Be"));
                session.store(stopWords);

                session.saveChanges();
                waitForIndexing(store);
            }

            String indexName = new DataIndex().getIndexName();

            try (IDocumentSession session = store.openSession()) {
                MoreLikeThisOptions options = new MoreLikeThisOptions();
                options.setMinimumTermFrequency(2);
                options.setMinimumDocumentFrequency(1);
                options.setStopWordsDocumentId("Config/Stopwords");

                List<Data> list = session.query(Data.class, DataIndex.class)
                        .moreLikeThis(f -> f.usingDocument(x -> x.whereEquals("id()", key)).withOptions(options))
                        .toList();

                assertThat(list)
                        .hasSize(5);
            }
        }
    }

    @Test
    public void canMakeDynamicDocumentQueries() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            new DataIndex().execute(store);

            try (IDocumentSession session = store.openSession()) {
                List<Data> list = getDataList();

                list.forEach(session::store);
                session.saveChanges();
            }

            waitForIndexing(store);

            try (IDocumentSession session = store.openSession()) {

                MoreLikeThisOptions options = new MoreLikeThisOptions();
                options.setFields(new String[]{ "body" });
                options.setMinimumTermFrequency(1);
                options.setMinimumDocumentFrequency(1);

                List<Data> list = session.query(Data.class, DataIndex.class)
                        .moreLikeThis(f -> f.usingDocument("{ \"body\": \"A test\" }").withOptions(options))
                        .toList();

                assertThat(list)
                        .hasSize(7);
            }
        }
    }

    @Test
    public void canMakeDynamicDocumentQueriesWithComplexProperties() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            new ComplexDataIndex().execute(store);

            try (IDocumentSession session = store.openSession()) {
                ComplexProperty complexProperty = new ComplexProperty();
                complexProperty.setBody("test");

                ComplexData complexData = new ComplexData();
                complexData.setProperty(complexProperty);

                session.store(complexData);
                session.saveChanges();
            }

            waitForIndexing(store);

            try (IDocumentSession session = store.openSession()) {
                MoreLikeThisOptions options = new MoreLikeThisOptions();
                options.setMinimumTermFrequency(1);
                options.setMinimumDocumentFrequency(1);

                List<ComplexData> list = session.query(ComplexData.class, ComplexDataIndex.class)
                        .moreLikeThis(f -> f.usingDocument("{ \"Property\": { \"Body\": \"test\" } }").withOptions(options))
                        .toList();

                assertThat(list)
                        .hasSize(1);
            }
        }
    }

    private static <T, TIndex extends AbstractIndexCreationTask> void assertMoreLikeThisHasMatchesFor(Class<T> clazz, Class<TIndex> indexClass, IDocumentStore store, String documentKey) {
        try (IDocumentSession session = store.openSession()) {
            MoreLikeThisOptions options = new MoreLikeThisOptions();
            options.setFields(new String[]{ "body" });
            List<T> list = session.query(clazz, indexClass)
                    .moreLikeThis(f -> f.usingDocument(b -> b.whereEquals("id()", documentKey)).withOptions(options))
                    .toList();

            assertThat(list)
                    .isNotEmpty();
        }
    }

    public static abstract class Identity {
        private String id;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }
    }

    public static class Data extends Identity {

        public Data() {
        }

        public Data(String body) {
            this.body = body;
        }

        private String body;
        private String whitespaceAnalyzerField;
        private String personId;

        public String getBody() {
            return body;
        }

        public void setBody(String body) {
            this.body = body;
        }

        public String getWhitespaceAnalyzerField() {
            return whitespaceAnalyzerField;
        }

        public void setWhitespaceAnalyzerField(String whitespaceAnalyzerField) {
            this.whitespaceAnalyzerField = whitespaceAnalyzerField;
        }

        public String getPersonId() {
            return personId;
        }

        public void setPersonId(String personId) {
            this.personId = personId;
        }
    }

    public static class DataWithIntegerId extends Identity {
        private String body;

        public String getBody() {
            return body;
        }

        public void setBody(String body) {
            this.body = body;
        }
    }

    public static class ComplexData {
        private String id;
        private ComplexProperty property;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public ComplexProperty getProperty() {
            return property;
        }

        public void setProperty(ComplexProperty property) {
            this.property = property;
        }
    }

    public static class ComplexProperty {
        private String body;

        public String getBody() {
            return body;
        }

        public void setBody(String body) {
            this.body = body;
        }
    }

    public static class DataIndex extends AbstractIndexCreationTask {
        public DataIndex() {
            this(true, false);
        }

        public DataIndex(boolean termVector, boolean store) {
            map = "from doc in docs.Datas select new { doc.body, doc.whitespaceAnalyzerField }";

            analyze("body", "Lucene.Net.Analysis.Standard.StandardAnalyzer");
            analyze("whitespaceAnalyzerField", "Lucene.Net.Analysis.WhitespaceAnalyzer");


            if (store) {
                store("body", FieldStorage.YES);
                store("whitespaceAnalyzerField", FieldStorage.YES);
            }

            if (termVector) {
                termVector("body", FieldTermVector.YES);
                termVector("whitespaceAnalyzerField", FieldTermVector.YES);
            }
        }
    }

    public static class ComplexDataIndex extends AbstractIndexCreationTask {
        public ComplexDataIndex() {
            map = "from doc in docs.ComplexDatas select new  { doc.property, doc.property.body }";

            index("body", FieldIndexing.SEARCH);
        }
    }
}
