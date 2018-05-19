package net.ravendb.client.documents;

import net.ravendb.client.documents.commands.StreamResult;

import java.io.Closeable;
import java.util.Iterator;

public interface CloseableIterator<T> extends Iterator<T>, Closeable {

    @Override
    void close();
}
