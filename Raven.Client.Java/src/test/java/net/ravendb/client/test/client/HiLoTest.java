package net.ravendb.client.test.client;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.identity.HiLoIdGenerator;
import net.ravendb.client.documents.session.IDocumentSession;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

public class HiLoTest extends RemoteTestBase { //TODO: extends replication test

    private static class HiloDoc {
        private long max;

        public long getMax() {
            return max;
        }

        public void setMax(long max) {
            this.max = max;
        }
    }

    private static class Product {
        private String productName;

        public String getProductName() {
            return productName;
        }

        public void setProductName(String productName) {
            this.productName = productName;
        }
    }

    @Test
    public void hiloCanNotGoDown() throws IOException {
        try (IDocumentStore store = getDocumentStore()) {

            try (IDocumentSession session = store.openSession()) {
                HiloDoc hiloDoc = new HiloDoc();
                hiloDoc.setMax(32);

                session.store(hiloDoc, "Raven/Hilo/users");
                session.saveChanges();

                HiLoIdGenerator hiLoKeyGenerator = new HiLoIdGenerator("users", store, store.getDatabase(), store.getConventions().getIdentityPartsSeparator());

                List<Long> ids = new ArrayList<>();
                ids.add(hiLoKeyGenerator.nextId());

                hiloDoc.setMax(12);
                session.store(hiloDoc, null, "Raven/Hilo/users");
                session.saveChanges();

                for (int i = 0; i < 128; i++) {
                    long nextId = hiLoKeyGenerator.nextId();
                    assertThat(ids)
                            .doesNotContain(nextId);
                    ids.add(nextId);
                }

                assertThat(new HashSet<>(ids))
                        .hasSize(ids.size());
            }
        }
    }

   /* TODO
   [Fact]
        public void HiLo_Async_MultiDb()
        {
            using (var store = GetDocumentStore())
            {
                using (var session = store.OpenSession())
                {
                    session.Store(new HiloDoc
                    {
                        Max = 64
                    }, "Raven/Hilo/users");

                    session.Store(new HiloDoc
                    {
                        Max = 128
                    }, "Raven/Hilo/products");

                    session.SaveChanges();


                    var multiDbHiLo = new AsyncMultiDatabaseHiLoIdGenerator(store, store.Conventions);

                    var generateDocumentKey = multiDbHiLo.GenerateDocumentIdAsync(null, new User()).GetAwaiter().GetResult();
                    Assert.Equal("users/65-A", generateDocumentKey);

                    generateDocumentKey = multiDbHiLo.GenerateDocumentIdAsync(null, new Product()).GetAwaiter().GetResult();
                    Assert.Equal("products/129-A", generateDocumentKey);
                }
            }
        }

        [Fact]
        public void Capacity_Should_Double()
        {
            using (var store = GetDocumentStore())
            {
                var hiLoKeyGenerator = new AsyncHiLoIdGenerator("users", store, store.Database,
                    store.Conventions.IdentityPartsSeparator);

                using (var session = store.OpenSession())
                {
                    session.Store(new HiloDoc
                    {
                        Max = 64
                    }, "Raven/Hilo/users");

                    session.SaveChanges();

                    for (var i = 0; i < 32; i++)
                        hiLoKeyGenerator.GenerateDocumentIdAsync(new User()).GetAwaiter().GetResult();
                }

                using (var session = store.OpenSession())
                {
                    var hiloDoc = session.Load<HiloDoc>("Raven/Hilo/Users");
                    var max = hiloDoc.Max;
                    Assert.Equal(max, 96);

                    //we should be receiving a range of 64 now
                    hiLoKeyGenerator.GenerateDocumentIdAsync(new User()).GetAwaiter().GetResult();
                }

                using (var session = store.OpenSession())
                {
                    var hiloDoc = session.Load<HiloDoc>("Raven/Hilo/users");
                    var max = hiloDoc.Max;
                    Assert.Equal(max, 160);
                }
            }
        }

        [Fact]
        public void Return_Unused_Range_On_Dispose()
        {
            using (var store = GetDocumentStore())
            {
                var newStore = new DocumentStore()
                {
                    Urls = store.Urls,
                    Database = store.Database
                };
                newStore.Initialize();

                using (var session = newStore.OpenSession())
                {
                    session.Store(new HiloDoc
                    {
                        Max = 32
                    }, "Raven/Hilo/users");

                    session.SaveChanges();

                    session.Store(new User());
                    session.Store(new User());

                    session.SaveChanges();
                }
                newStore.Dispose(); //on document store dispose, hilo-return should be called

                newStore = new DocumentStore()
                {
                    Urls = store.Urls,
                    Database = store.Database
                };
                newStore.Initialize();

                using (var session = newStore.OpenSession())
                {
                    var hiloDoc = session.Load<HiloDoc>("Raven/Hilo/users");
                    var max = hiloDoc.Max;
                    Assert.Equal(34, max);
                }
                newStore.Dispose();
            }
        }

        [Fact]
        public async Task Should_Resolve_Conflict_With_Highest_Number()
        {
            using (var store1 = GetDocumentStore(new Options { ModifyDatabaseName = s => s + "_foo1" }))
            using (var store2 = GetDocumentStore(new Options { ModifyDatabaseName = s => s + "_foo2" }))
            {
                using (var s1 = store1.OpenSession())
                {
                    var hiloDoc = new HiloDoc
                    {
                        Max = 128
                    };
                    s1.Store(hiloDoc, "Raven/Hilo/users");
                    s1.Store(new User(), "marker/doc");
                    s1.SaveChanges();
                }
                using (var s2 = store2.OpenSession())
                {
                    var hiloDoc2 = new HiloDoc
                    {
                        Max = 64
                    };
                    s2.Store(hiloDoc2, "Raven/Hilo/users");
                    s2.SaveChanges();
                }

                await SetupReplicationAsync(store1, store2);

                WaitForMarkerDocumentAndAllPrecedingDocumentsToReplicate(store2);

                var nextId = new AsyncHiLoIdGenerator("users", store2, store2.Database,
                    store2.Conventions.IdentityPartsSeparator).NextIdAsync().GetAwaiter().GetResult();
                Assert.Equal(nextId, 129);
            }
        }

        private static void WaitForMarkerDocumentAndAllPrecedingDocumentsToReplicate(DocumentStore store2)
        {
            var sp = Stopwatch.StartNew();
            while (true)
            {
                using (var session = store2.OpenSession())
                {
                    if (session.Load<object>("marker/doc") != null)
                        break;
                    Thread.Sleep(32);
                }
                if (sp.Elapsed.TotalSeconds > (Debugger.IsAttached ? 60 * 1024 : 30))
                    throw new TimeoutException("waited too long");
            }
        }

        //hilo concurrency tests

        private const int GeneratedIdCount = 2000;
        private const int ThreadCount = 100;

        [Fact]
        public void ParallelGeneration_NoClashesOrGaps()
        {
            using (var store = GetDocumentStore())
            {
                var gen = new AsyncHiLoIdGenerator("When_generating_lots_of_keys_concurrently_there_are_no_clashes", store,
                    store.Database, store.Conventions.IdentityPartsSeparator);
                ConcurrencyTester(() => gen.NextIdAsync().GetAwaiter().GetResult(), ThreadCount, GeneratedIdCount);
            }
        }

        [Fact]
        public void SequentialGeneration_NoClashesOrGaps()
        {
            using (var store = GetDocumentStore())
            {
                var gen = new AsyncHiLoIdGenerator("When_generating_lots_of_keys_concurrently_there_are_no_clashes", store,
                    store.Database, store.Conventions.IdentityPartsSeparator);
                ConcurrencyTester(() => gen.NextIdAsync().GetAwaiter().GetResult(), 1, GeneratedIdCount);
            }
        }

        private static void ConcurrencyTester(Func<long> generate, int threadCount, int generatedIdCount)
        {
            var waitingThreadCount = 0;
            var starterGun = new ManualResetEvent(false);

            var results = new long[generatedIdCount];
            var threads = Enumerable.Range(0, threadCount).Select(threadNumber => new Thread(() =>
            {
                // Wait for all threads to be ready
                Interlocked.Increment(ref waitingThreadCount);
                starterGun.WaitOne();

                for (var i = threadNumber; i < generatedIdCount; i += threadCount)
                    results[i] = generate();
            })).ToArray();

            foreach (var t in threads)
                t.Start();

            // Wait for all tasks to reach the waiting stage
            var wait = new SpinWait();
            while (waitingThreadCount < threadCount)
                wait.SpinOnce();

            // Start all the threads at the same time
            starterGun.Set();
            foreach (var t in threads)
                t.Join();

            var ids = new HashSet<long>();
            foreach (var value in results)
            {
                if (!ids.Add(value))
                {
                    throw new Exception("Id " + value + " was generated more than once, in indices "
                        + string.Join(", ", results.Select(Tuple.Create<long, int>).Where(x => x.Item1 == value).Select(x => x.Item2)));
                }
            }

            for (long i = 1; i <= GeneratedIdCount; i++)
                Assert.True(ids.Contains(i), "Id " + i + " was not generated.");
        }

        [Fact]
        public void HiLoKeyGenerator_works_without_aggressive_caching()
        {
            using (var store = GetDocumentStore())
            {
                var hiLoKeyGenerator = new AsyncHiLoIdGenerator("users", store, store.Database,
                    store.Conventions.IdentityPartsSeparator);

                Assert.Equal(1L, hiLoKeyGenerator.NextIdAsync().GetAwaiter().GetResult());
                Assert.Equal(2L, hiLoKeyGenerator.NextIdAsync().GetAwaiter().GetResult());
            }
        }

        [Fact]
        public async Task HiLoKeyGenerator_async_hangs_when_aggressive_caching_enabled()
        {
            using (var store = GetDocumentStore())
            {
                using (store.AggressivelyCache())
                {
                    var hiLoKeyGenerator = new AsyncHiLoIdGenerator("users", store, store.Database,
                        store.Conventions.IdentityPartsSeparator);

                    Assert.Equal(1L, await hiLoKeyGenerator.NextIdAsync());
                    Assert.Equal(2L, await hiLoKeyGenerator.NextIdAsync());
                }
            }
        }

        [Fact]
        public void HiLoKeyGenerator_hangs_when_aggressive_caching_enabled()
        {
            using (var store = GetDocumentStore())
            {
                using (store.AggressivelyCache())
                {
                    var hiLoKeyGenerator = new AsyncHiLoIdGenerator("users", store, store.Database,
                        store.Conventions.IdentityPartsSeparator);

                    Assert.Equal(1L, hiLoKeyGenerator.NextIdAsync().GetAwaiter().GetResult());
                    Assert.Equal(2L, hiLoKeyGenerator.NextIdAsync().GetAwaiter().GetResult());
                }
            }
        }

        [Fact]
        public void HiLoKeyGenerator_hangs_when_aggressive_caching_enabled_on_other_documentstore()
        {
            using (var server = GetNewServer())
            using (var otherServer = GetNewServer())
            using (var store = GetDocumentStore(new Options { Server = server }))
            using (var otherStore = GetDocumentStore(new Options { Server = otherServer }))
            {
                using (otherStore.AggressivelyCache()) // Note that we don't even use the other store, we just call AggressivelyCache on it
                {
                    var hilo = new AsyncHiLoIdGenerator("users", store, store.Database,
                        store.Conventions.IdentityPartsSeparator);
                    Assert.Equal(1L, hilo.NextIdAsync().GetAwaiter().GetResult());
                    Assert.Equal(2L, hilo.NextIdAsync().GetAwaiter().GetResult());
                }
            }
        }

        [Fact]
        public async Task HiLoKeyGenerator_async_hangs_when_aggressive_caching_enabled_on_other_documentstore()
        {
            using (var server = GetNewServer())
            using (var otherServer = GetNewServer())
            using (var store = GetDocumentStore(new Options { Server = server }))
            using (var otherStore = GetDocumentStore(new Options { Server = otherServer }))
            {
                using (otherStore.AggressivelyCache()) // Note that we don't even use the other store, we just call AggressivelyCache on it
                {
                    var hilo = new AsyncHiLoIdGenerator("users", store, store.Database,
                        store.Conventions.IdentityPartsSeparator);
                    Assert.Equal(1L, await hilo.NextIdAsync());
                    Assert.Equal(2L, await hilo.NextIdAsync());
                }
            }
        }
    }
    */
}
