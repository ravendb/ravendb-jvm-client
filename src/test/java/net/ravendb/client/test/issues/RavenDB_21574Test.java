package net.ravendb.client.test.issues;

import net.ravendb.client.Constants;
import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.DocumentStore;
import net.ravendb.client.documents.indexes.*;
import net.ravendb.client.documents.operations.indexes.GetIndexOperation;
import net.ravendb.client.documents.operations.indexes.GetIndexStatisticsOperation;
import net.ravendb.client.documents.operations.indexes.PutIndexesOperation;
import net.ravendb.client.primitives.SharpEnum;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

public class RavenDB_21574Test extends RemoteTestBase {

    public static class CompanyIndex extends AbstractIndexCreationTask {
        public CompanyIndex() {
            map = "from company in docs.Companies select new { company.name }";
            searchEngineType = SearchEngineType.CORAX;
        }
    }

    public static class CompanyIndex_MultiMap extends AbstractMultiMapIndexCreationTask {
        public CompanyIndex_MultiMap() {
            addMap("from company in docs.Companies select new { company.name }");
            reduce = "from result in results " +
                    "group result by result.name " +
                    "into g " +
                    "select new " +
                    "{ " +
                    "  name = g.Key " +
                    "}";

            searchEngineType = SearchEngineType.CORAX;
        }
    }

    public static class CompanyIndex_JavaScript extends AbstractJavaScriptIndexCreationTask {
        @Override
        public String getIndexName() {
            return "Companies/JavaScript";
        }

        public CompanyIndex_JavaScript() {
            setMaps(Collections.singleton("map('Companies', function(company) { return { Name: company.Name } });"));

            searchEngineType = SearchEngineType.CORAX;
        }
    }

    @Test
    public void setting_Index_SearchEngineType_Should_Work() throws Exception {
        try (DocumentStore store = getDocumentStore()) {
            validateSearchEngineType(CompanyIndex.class, store);
            validateSearchEngineType(CompanyIndex_MultiMap.class, store);
            validateSearchEngineType(CompanyIndex_JavaScript.class, store);
        }
    }

    private <T extends AbstractIndexCreationTaskBase> void validateSearchEngineType(Class<T> clazz, DocumentStore store) throws Exception {
        T index = clazz.getDeclaredConstructor().newInstance();
        IndexCreation.createIndexes(Collections.singletonList(index), store);

        waitForIndexing(store);

        IndexDefinition indexDefinition = store.maintenance().send(new GetIndexOperation(index.getIndexName()));
        IndexStats indexStats = store.maintenance().send(new GetIndexStatisticsOperation(index.getIndexName()));

        assertThat(indexDefinition.getConfiguration().get(Constants.Configuration.Indexes.INDEXING_STATIC_SEARCH_ENGINE_TYPE))
                .isEqualTo(SharpEnum.value(SearchEngineType.CORAX));
        assertThat(indexStats.getSearchEngineType())
                .isEqualTo(SearchEngineType.CORAX);

        index.setSearchEngineType(SearchEngineType.LUCENE);

        IndexCreation.createIndexes(Collections.singletonList(index), store);
        waitForIndexing(store);

        indexDefinition = store.maintenance().send(new GetIndexOperation(index.getIndexName()));
        indexStats = store.maintenance().send(new GetIndexStatisticsOperation(index.getIndexName()));

        assertThat(indexDefinition.getConfiguration().get(Constants.Configuration.Indexes.INDEXING_STATIC_SEARCH_ENGINE_TYPE))
                .isEqualTo(SharpEnum.value(SearchEngineType.LUCENE));
        assertThat(indexStats.getSearchEngineType())
                .isEqualTo(SearchEngineType.LUCENE);

        index.setSearchEngineType(SearchEngineType.CORAX);
        index.execute(store);
        waitForIndexing(store);

        indexDefinition = store.maintenance().send(new GetIndexOperation(index.getIndexName()));
        indexStats = store.maintenance().send(new GetIndexStatisticsOperation(index.getIndexName()));

        assertThat(indexDefinition.getConfiguration().get(Constants.Configuration.Indexes.INDEXING_STATIC_SEARCH_ENGINE_TYPE))
                .isEqualTo(SharpEnum.value(SearchEngineType.CORAX));
        assertThat(indexStats.getSearchEngineType())
                .isEqualTo(SearchEngineType.CORAX);

        index.setSearchEngineType(SearchEngineType.LUCENE);
        store.executeIndex(index);
        waitForIndexing(store);

        indexDefinition = store.maintenance().send(new GetIndexOperation(index.getIndexName()));
        indexStats = store.maintenance().send(new GetIndexStatisticsOperation(index.getIndexName()));

        assertThat(indexDefinition.getConfiguration().get(Constants.Configuration.Indexes.INDEXING_STATIC_SEARCH_ENGINE_TYPE))
                .isEqualTo(SharpEnum.value(SearchEngineType.LUCENE));
        assertThat(indexStats.getSearchEngineType())
                .isEqualTo(SearchEngineType.LUCENE);

        index = clazz.getDeclaredConstructor().newInstance();
        indexDefinition = index.createIndexDefinition();
        store.maintenance().send(new PutIndexesOperation(indexDefinition));
        waitForIndexing(store);

        indexDefinition = store.maintenance().send(new GetIndexOperation(index.getIndexName()));
        indexStats = store.maintenance().send(new GetIndexStatisticsOperation(index.getIndexName()));

        assertThat(indexDefinition.getConfiguration().get(Constants.Configuration.Indexes.INDEXING_STATIC_SEARCH_ENGINE_TYPE))
                .isEqualTo(SharpEnum.value(SearchEngineType.CORAX));
        assertThat(indexStats.getSearchEngineType())
                .isEqualTo(SearchEngineType.CORAX);
    }

}
