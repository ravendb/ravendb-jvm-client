package net.ravendb.client.shard;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

import net.ravendb.abstractions.closure.Function2;
import net.ravendb.abstractions.exceptions.AggregateException;
import net.ravendb.client.connection.IDatabaseCommands;

/**
 * Apply an operation to all the shard session in sequence
 */
public class SequentialShardAccessStrategy implements IShardAccessStrategy {

  private List<ShardingErrorHandle<IDatabaseCommands>> onError = new ArrayList<>();

  @Override
  public void addOnError(ShardingErrorHandle<IDatabaseCommands> handler) {
    onError.add(handler);
  }

  @Override
  public void removeOnError(ShardingErrorHandle<IDatabaseCommands> handler) {
    onError.remove(handler);
  }

  /**
   * Applies the specified action for all shard sessions.
   */
  @SuppressWarnings({"unchecked", "boxing"})
  @Override
  public <T> T[] apply(Class<?> clazz, List<IDatabaseCommands> commands, ShardRequestData request,
    Function2<IDatabaseCommands, Integer, T> operation) {
    List<T> list = new ArrayList<>();
    List<Exception> errors = new ArrayList<>();
    for (int i = 0; i < commands.size(); i++) {
      try {
        list.add(operation.apply(commands.get(i), i));
      } catch (Exception e) {
        if (onError.isEmpty()) {
          throw e;
        }
        for (int j = 0; j < onError.size(); j++) {
          Boolean result = onError.get(j).apply(commands.get(i), request, e);
          if (j == onError.size() - 1 && !result) {
            throw e;
          }
        }
        errors.add(e);
      }
    }

    // if ALL nodes failed, we still throw
    if (errors.size() == commands.size()) {
      throw new AggregateException(errors.toArray(new Exception[0]));
    }
    return list.toArray((T[]) Array.newInstance(clazz, 0));
  }
}
