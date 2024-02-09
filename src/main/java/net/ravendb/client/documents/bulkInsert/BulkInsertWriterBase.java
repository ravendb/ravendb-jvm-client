package net.ravendb.client.documents.bulkInsert;

import net.ravendb.client.documents.BulkInsertOperation;

import java.io.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;

public abstract class BulkInsertWriterBase implements Closeable {

    private final ExecutorService _executorService;

    protected final int _maxSizeInBuffer = 1024 * 1024;

    private CompletableFuture<Void> _asyncWrite = CompletableFuture.completedFuture(null);

    protected Writer _currentWriteStream;
    private Writer _backgroundWriteStream;

    private ByteArrayOutputStream _memoryBuffer;
    private ByteArrayOutputStream _backgroundMemoryBuffer;

    private boolean _isInitialWrite = true;

    public OutputStream _requestBodyStream;

    public final BulkInsertOperation.BulkInsertStreamExposerContent streamExposer;

    protected BulkInsertWriterBase(ExecutorService executorService) {
        _executorService = executorService;
        streamExposer = new BulkInsertOperation.BulkInsertStreamExposerContent();

        _memoryBuffer = new ByteArrayOutputStream();
        _backgroundMemoryBuffer = new ByteArrayOutputStream();

        _currentWriteStream = new OutputStreamWriter(_memoryBuffer);
        _backgroundWriteStream = new OutputStreamWriter(_backgroundMemoryBuffer);

        _asyncWrite = CompletableFuture.completedFuture(null);
    }

    public void initialize() {
        onCurrentWriteStreamSet(_memoryBuffer);
    }

    public Writer getWriter() {
        return _currentWriteStream;
    }

    public ByteArrayOutputStream getBuffer() {
        return _memoryBuffer;
    }

    public boolean flushIfNeeded() throws IOException, ExecutionException, InterruptedException {
        return flushIfNeeded(false);
    }

    public boolean flushIfNeeded(boolean force) throws IOException, ExecutionException, InterruptedException {
        if (_memoryBuffer.size() > _maxSizeInBuffer || _asyncWrite.isDone() || force) {
            _asyncWrite.get();

            Writer tmp = _currentWriteStream;
            _currentWriteStream = _backgroundWriteStream;
            _backgroundWriteStream = tmp;

            ByteArrayOutputStream tmpBaos = _memoryBuffer;
            _memoryBuffer = _backgroundMemoryBuffer;
            _backgroundMemoryBuffer = tmpBaos;

            _memoryBuffer.reset();

            onCurrentWriteStreamSet(_memoryBuffer);

            final byte[] buffer = _backgroundMemoryBuffer.toByteArray();
            _asyncWrite = writeToStream(_requestBodyStream, buffer, _isInitialWrite || force);
            _isInitialWrite = false;
            return _isInitialWrite || force;
        }

        return false;
    }

    protected void onCurrentWriteStreamSet(ByteArrayOutputStream baos) {
        // empty
    }

    private CompletableFuture<Void> writeToStream(OutputStream dst, byte[] buffer, boolean forceFlush) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                dst.write(buffer);

                if (forceFlush) {
                    // send this chunk
                    _requestBodyStream.flush();
                }

            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return null;
        }, _executorService);
    }

    public void ensureStream() throws IOException, InterruptedException, ExecutionException {
        _requestBodyStream = streamExposer.outputStream.get();

        _currentWriteStream.write('[');
    }


    @Override
    public void close() throws IOException {

        if (this.streamExposer.isDone()) {
            return;
        }

        try {
            if (_requestBodyStream != null) {
                _currentWriteStream.write("]");
                _currentWriteStream.flush();

                try {
                    _asyncWrite.get();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }

                byte[] buffer = _memoryBuffer.toByteArray();
                _requestBodyStream.write(buffer);
                _requestBodyStream.flush();
            }
        } finally {
            streamExposer.done();
        }
    }
}
