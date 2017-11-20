package net.ravendb.client.documents.indexes;

import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.session.operations.QueryOperation;
import net.ravendb.client.primitives.Lang;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.List;

public class IndexCreation {
    private static final Log logger = LogFactory.getLog(IndexCreation.class);

    /* TODO
     /// <summary>
        /// Creates the indexes found in the specified assembly.
        /// </summary>
        public static void CreateIndexes(Assembly assemblyToScan, IDocumentStore store, DocumentConventions conventions = null)
        {
            AsyncHelpers.RunSync(() => CreateIndexesAsync(assemblyToScan, store, conventions));
        }

        /// <summary>
        /// Creates the indexes found in the specified assembly.
        /// </summary>
        public static Task CreateIndexesAsync(Assembly assemblyToScan, IDocumentStore store, DocumentConventions conventions = null, CancellationToken token = default(CancellationToken))
        {
            var indexes = GetAllInstancesOfType<AbstractIndexCreationTask>(assemblyToScan);

            return CreateIndexesAsync(indexes, store, conventions, token);
        }

        public static void CreateIndexes(IEnumerable<AbstractIndexCreationTask> indexes, IDocumentStore store, DocumentConventions conventions = null)
        {
            AsyncHelpers.RunSync(() => CreateIndexesAsync(indexes, store, conventions));
        }

        public static async Task CreateIndexesAsync(IEnumerable<AbstractIndexCreationTask> indexes,IDocumentStore store, DocumentConventions conventions = null, CancellationToken token = default(CancellationToken))
        {
            var indexesList = indexes?.ToList() ?? new List<AbstractIndexCreationTask>();

            if (conventions == null)
                conventions = store.Conventions;

            var indexCompilationExceptions = new List<IndexCompilationException>();
            try
            {
                var indexesToAdd = CreateIndexesToAdd(indexesList, conventions);
                await store.Admin.SendAsync(new PutIndexesOperation(indexesToAdd), token).ConfigureAwait(false);
            }
            // For old servers that don't have the new endpoint for executing multiple indexes
            catch (Exception ex)
            {
                if (_logger.IsInfoEnabled)
                {
                    _logger.Info("Could not create indexes in one shot (maybe using older version of RavenDB ?)", ex);
                }

                foreach (var task in indexesList)
                {
                    try
                    {
                        await task.ExecuteAsync(store, conventions, token).ConfigureAwait(false);
                    }
                    catch (IndexCompilationException e)
                    {
                        indexCompilationExceptions.Add(new IndexCompilationException("Failed to compile index name = " + task.IndexName, e));
                    }
                }
            }

            if (indexCompilationExceptions.Any())
                throw new AggregateException("Failed to create one or more indexes. Please see inner exceptions for more details.", indexCompilationExceptions);
        }
*/

    public static IndexDefinition[] createIndexesToAdd(List<AbstractIndexCreationTask> indexCreationTasks, DocumentConventions conventions) {
        return indexCreationTasks.stream()
                .map(x -> {
                    x.setConventions(conventions);
                    IndexDefinition definition = x.createIndexDefinition();
                    definition.setName(x.getIndexName());
                    definition.setPriority(Lang.coalesce(x.getPriority(), IndexPriority.NORMAL));
                    return definition;
                }).toArray(IndexDefinition[]::new);
    }

    /*TODO

        private static IEnumerable<TType> GetAllInstancesOfType<TType>(Assembly assembly)
        {
            foreach (var type in assembly.GetTypes()
                .Where(x =>
                x.GetTypeInfo().IsClass &&
                x.GetTypeInfo().IsAbstract == false &&
                x.GetTypeInfo().IsSubclassOf(typeof(TType))))
            {
                yield return (TType)Activator.CreateInstance(type);
            }
        }
    }
     */
}
