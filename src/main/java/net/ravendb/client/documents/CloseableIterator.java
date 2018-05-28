package net.ravendb.client.documents;

import java.io.Closeable;
import java.util.Iterator;

public interface CloseableIterator<T> extends Iterator<T>, Closeable {

    @Override
    void close();
}
