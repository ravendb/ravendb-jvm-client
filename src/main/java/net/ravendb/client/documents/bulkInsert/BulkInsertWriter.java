package net.ravendb.client.documents.bulkInsert;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;

public class BulkInsertWriter extends BulkInsertWriterBase {


    public BulkInsertWriter(ExecutorService executorService) {
        super(executorService);
    }

    public void flushIfNeeded() throws IOException, ExecutionException, InterruptedException {
        flushIfNeeded(false);
    }

    public void flushIfNeeded(boolean force) throws IOException, ExecutionException, InterruptedException {
        _currentWriteStream.flush();

        super.flushIfNeeded(force);
    }

    public void write(byte[] bytes) throws IOException {
        getBuffer().write(bytes);
    }

    public void write(String value) throws IOException {
        _currentWriteStream.write(value);
    }

    public void write(int value) throws IOException {
        _currentWriteStream.write(value);
    }

    public void write(char value) throws IOException {
        _currentWriteStream.write(value);
    }

    public void flush() throws IOException {
        _currentWriteStream.flush();
    }

    @Override
    public void close() throws IOException {
        try {
            _currentWriteStream.flush();
            super.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
