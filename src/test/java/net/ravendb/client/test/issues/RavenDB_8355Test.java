package net.ravendb.client.test.issues;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.DocumentStore;
import net.ravendb.client.documents.operations.sorters.DeleteSorterOperation;
import net.ravendb.client.documents.operations.sorters.PutSortersOperation;
import net.ravendb.client.documents.queries.sorting.SorterDefinition;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.exceptions.RavenException;
import net.ravendb.client.exceptions.documents.compilation.SorterCompilationException;
import net.ravendb.client.exceptions.documents.sorters.SorterDoesNotExistException;
import net.ravendb.client.infrastructure.entities.Company;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class RavenDB_8355Test extends RemoteTestBase {

    public static String sorterCode = "using System;\n" +
            "using System.Collections.Generic;\n" +
            "using Lucene.Net.Index;\n" +
            "using Lucene.Net.Search;\n" +
            "using Lucene.Net.Store;\n" +
            "\n" +
            "namespace SlowTests.Data.RavenDB_8355\n" +
            "{\n" +
            "    public class MySorter : FieldComparator\n" +
            "    {\n" +
            "        private readonly string _args;\n" +
            "\n" +
            "        public MySorter(string fieldName, int numHits, int sortPos, bool reversed, List<string> diagnostics)\n" +
            "        {\n" +
            "            _args = $\"{fieldName}:{numHits}:{sortPos}:{reversed}\";\n" +
            "        }\n" +
            "\n" +
            "        public override int Compare(int slot1, int slot2)\n" +
            "        {\n" +
            "            throw new InvalidOperationException($\"Catch me: {_args}\");\n" +
            "        }\n" +
            "\n" +
            "        public override void SetBottom(int slot)\n" +
            "        {\n" +
            "            throw new InvalidOperationException($\"Catch me: {_args}\");\n" +
            "        }\n" +
            "\n" +
            "        public override int CompareBottom(int doc, IState state)\n" +
            "        {\n" +
            "            throw new InvalidOperationException($\"Catch me: {_args}\");\n" +
            "        }\n" +
            "\n" +
            "        public override void Copy(int slot, int doc, IState state)\n" +
            "        {\n" +
            "            throw new InvalidOperationException($\"Catch me: {_args}\");\n" +
            "        }\n" +
            "\n" +
            "        public override void SetNextReader(IndexReader reader, int docBase, IState state)\n" +
            "        {\n" +
            "            throw new InvalidOperationException($\"Catch me: {_args}\");\n" +
            "        }\n" +
            "\n" +
            "        public override IComparable this[int slot] => throw new InvalidOperationException($\"Catch me: {_args}\");\n" +
            "    }\n" +
            "}\n";


    @Test
    public void canUseCustomSorter() throws Exception {
        SorterDefinition sorterDefinition = new SorterDefinition();
        sorterDefinition.setName("MySorter");
        sorterDefinition.setCode(sorterCode);

        PutSortersOperation operation = new PutSortersOperation(sorterDefinition);

        try (DocumentStore store = getDocumentStore()) {
            store.maintenance().send(operation);

            try (IDocumentSession session = store.openSession()) {
                Company company1 = new Company();
                company1.setName("C1");
                session.store(company1);

                Company company2 = new Company();
                company2.setName("C2");
                session.store(company2);

                session.saveChanges();
            }

            canUseSorterInternal(RavenException.class, store, "Catch me: name:2:0:False", "Catch me: name:2:0:True");
        }
    }

    @Test
    public void canUseCustomSorterWithOperations() throws Exception {
        try (DocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                Company company1 = new Company();
                company1.setName("C1");
                session.store(company1);

                Company company2 = new Company();
                company2.setName("C2");
                session.store(company2);

                session.saveChanges();
            }

            canUseSorterInternal(SorterDoesNotExistException.class, store, "There is no sorter with 'MySorter' name", "There is no sorter with 'MySorter' name");

            SorterDefinition sorterDefinition = new SorterDefinition();
            sorterDefinition.setName("MySorter");
            sorterDefinition.setCode(sorterCode);

            PutSortersOperation operation = new PutSortersOperation(sorterDefinition);
            store.maintenance().send(operation);

            // checking if we can send again same sorter
            store.maintenance().send(new PutSortersOperation(sorterDefinition));

            canUseSorterInternal(RavenException.class, store, "Catch me: name:2:0:False", "Catch me: name:2:0:True");

            sorterCode = sorterCode.replaceAll("Catch me", "Catch me 2");

            // checking if we can update sorter
            SorterDefinition sorterDefinition2 = new SorterDefinition();
            sorterDefinition2.setName("MySorter");
            sorterDefinition2.setCode(sorterCode);
            store.maintenance().send(new PutSortersOperation(sorterDefinition2));

            assertThatThrownBy(() -> {
                // We should not be able to add sorter with non-matching name
                SorterDefinition otherDefinition = new SorterDefinition();
                otherDefinition.setName("MySorter_OtherName");
                otherDefinition.setCode(sorterCode);
                store.maintenance().send(new PutSortersOperation(otherDefinition));
            })
                    .isInstanceOf(SorterCompilationException.class)
                    .hasMessageContaining("Could not find type 'MySorter_OtherName' in given assembly.");

            canUseSorterInternal(RavenException.class, store, "Catch me 2: name:2:0:False", "Catch me 2: name:2:0:True");

            store.maintenance().send(new DeleteSorterOperation("MySorter"));

            canUseSorterInternal(SorterDoesNotExistException.class, store, "There is no sorter with 'MySorter' name", "There is no sorter with 'MySorter' name");
        }
    }

    private static void canUseSorterInternal(Class<? extends RavenException> expectedClass, DocumentStore store, String asc, String desc) {
        try (IDocumentSession session = store.openSession()) {
            assertThatThrownBy(() -> {
                session
                        .advanced()
                        .rawQuery(Company.class, "from Companies order by custom(name, 'MySorter')")
                        .toList();
            })
                .isInstanceOf(expectedClass)
                .hasMessageContaining(asc);

            assertThatThrownBy(() -> {
                session
                        .query(Company.class)
                        .orderBy("name", "MySorter")
                        .toList();
            })
                .isInstanceOf(expectedClass)
                .hasMessageContaining(asc);

            assertThatThrownBy(() -> {
                session
                        .advanced()
                        .rawQuery(Company.class, "from Companies order by custom(name, 'MySorter') desc")
                        .toList();
            })
                    .isInstanceOf(expectedClass)
                    .hasMessageContaining(desc);

            assertThatThrownBy(() -> {
                session
                        .query(Company.class)
                        .orderByDescending("name", "MySorter")
                        .toList();
            })
                    .isInstanceOf(expectedClass)
                    .hasMessageContaining(desc);
        }
    }
}
