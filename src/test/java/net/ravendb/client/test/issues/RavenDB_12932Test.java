package net.ravendb.client.test.issues;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.indexes.AbstractIndexCreationTask;
import net.ravendb.client.documents.indexes.IndexDefinition;
import net.ravendb.client.documents.operations.indexes.GetIndexOperation;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class RavenDB_12932Test extends RemoteTestBase {

    @Test
    public void canPersistPatternForOutputReduceToCollectionReferences() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            Orders_ProfitByProductAndOrderedAt indexToCreate = new Orders_ProfitByProductAndOrderedAt("CustomCollection");
            indexToCreate.execute(store);

            IndexDefinition indexDefinition = store.maintenance().send(new GetIndexOperation("Orders/ProfitByProductAndOrderedAt"));

            assertThat(indexDefinition.getOutputReduceToCollection())
                    .isEqualTo("Profits");
            assertThat(indexDefinition.getPatternForOutputReduceToCollectionReferences())
                    .isEqualTo("reports/daily/{OrderedAt:yyyy-MM-dd}");
            assertThat(indexDefinition.getPatternReferencesCollectionName())
                    .isEqualTo("CustomCollection");
        }
    }

    public static class Orders_ProfitByProductAndOrderedAt extends AbstractIndexCreationTask {
        public Orders_ProfitByProductAndOrderedAt(String referencesCollectionName) {

            map = "docs.Orders.SelectMany(order => order.Lines, (order, line) => new {\n" +
                    "    Product = line.Product,\n" +
                    "    OrderedAt = order.OrderedAt,\n" +
                    "    Profit = (((decimal) line.Quantity) * line.PricePerUnit) * (1M - line.Discount)\n" +
                    "})";

            reduce = "results.GroupBy(r => new {\n" +
                    "    OrderedAt = r.OrderedAt,\n" +
                    "    Product = r.Product\n" +
                    "}).Select(g => new {\n" +
                    "    Product = g.Key.Product,\n" +
                    "    OrderedAt = g.Key.OrderedAt,\n" +
                    "    Profit = Enumerable.Sum(g, r => ((decimal) r.Profit))\n" +
                    "})";

            outputReduceToCollection = "Profits";

            patternForOutputReduceToCollectionReferences = "reports/daily/{OrderedAt:yyyy-MM-dd}";

            patternReferencesCollectionName = referencesCollectionName;
        }
    }
}
