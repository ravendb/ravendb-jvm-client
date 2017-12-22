package net.ravendb.client.primitives;

import java.io.Closeable;


public interface CleanCloseable extends Closeable {
    @Override
    void close();
}
