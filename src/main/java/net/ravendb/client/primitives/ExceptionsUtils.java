package net.ravendb.client.primitives;

import java.util.concurrent.ExecutionException;

public class ExceptionsUtils {

    public static <T> T accept(ThrowingSupplier<T> action) {
        try {
            return action.get();
        } catch (Exception e) {
            throw unwrapException(e);
        }
    }

    public static RuntimeException unwrapException(Throwable e) {

        if (e instanceof ExecutionException) {
            ExecutionException computationException = (ExecutionException) e;
            return unwrapException(computationException.getCause());
        }

        if (e instanceof RuntimeException) {
            return (RuntimeException)e;
        }

        return new RuntimeException(e.getMessage(), e);
    }
}
