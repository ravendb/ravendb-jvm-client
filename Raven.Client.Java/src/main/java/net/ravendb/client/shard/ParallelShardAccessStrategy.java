package net.ravendb.client.shard;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import net.ravendb.abstractions.basic.CleanCloseable;
import net.ravendb.abstractions.closure.Function2;
import net.ravendb.abstractions.exceptions.AggregateException;
import net.ravendb.client.connection.IDatabaseCommands;


/**
 * ParallelShardAccessStrategy
 * NOTE: Remember to close this instance!
 *
 */
public class ParallelShardAccessStrategy implements IShardAccessStrategy, CleanCloseable {

  public ParallelShardAccessStrategy() {
    super();
    threadPool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
  }

  private List<ShardingErrorHandle<IDatabaseCommands>> onError = new ArrayList<>();
  private ExecutorService threadPool;


  @Override
  public void addOnError(ShardingErrorHandle<IDatabaseCommands> handler) {
    onError.add(handler);
  }

  @Override
  public void removeOnError(ShardingErrorHandle<IDatabaseCommands> handler) {
    onError.remove(handler);
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T> T[] apply(Class<?> clazz, final List<IDatabaseCommands> commands, final ShardRequestData request,
    final Function2<IDatabaseCommands, Integer, T> operation) {
    final T[] returnedLists = (T[]) Array.newInstance(clazz, commands.size());
    final boolean[] valueSet = new boolean[commands.size()];
    final Exception[] errors = new Exception[commands.size()];

    Collection<Callable<Void>> tasks = new ArrayList<>();
    for (int i = 0; i < commands.size(); i++) {
      final int copy = i;
      final IDatabaseCommands cmd = commands.get(i);
      tasks.add(new Callable<Void>() {
        @SuppressWarnings({"boxing", "synthetic-access"})
        @Override
        public Void call() throws Exception {
          try {
            T value = operation.apply(cmd, copy);
            returnedLists[copy] = value;
            valueSet[copy] = true;
            return null;
          } catch (Exception e) {
            if (onError.isEmpty()) {
              throw e;
            }
            for (ShardingErrorHandle<IDatabaseCommands> handler: onError) {
              if (!handler.apply(commands.get(copy), request, e)) {
                throw e;
              }
            }
            errors[copy] = e;
          }
          return null;
        }
      });
    }
    try {
      threadPool.invokeAll(tasks);

      boolean allErrored = false;
      for (Exception e : errors) {
        allErrored &= e != null;
      }
      if (allErrored) {
        throw new AggregateException(errors);
      }

      List<T> result = new ArrayList<>();
      for (int i = 0; i < valueSet.length; i++) {
        if (valueSet[i]) {
          result.add(returnedLists[i]);
        }
      }

      return result.toArray((T[]) Array.newInstance(clazz, 0));

    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void close() {
    threadPool.shutdown();
  }
}
