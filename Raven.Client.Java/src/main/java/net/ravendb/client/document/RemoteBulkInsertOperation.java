package net.ravendb.client.document;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.Thread.UncaughtExceptionHandler;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPOutputStream;

import net.ravendb.abstractions.closure.Action1;
import net.ravendb.abstractions.data.BulkInsertChangeNotification;
import net.ravendb.abstractions.data.BulkInsertOptions;
import net.ravendb.abstractions.data.Constants;
import net.ravendb.abstractions.data.DocumentChangeTypes;
import net.ravendb.abstractions.data.HttpMethods;
import net.ravendb.abstractions.json.linq.RavenJObject;
import net.ravendb.abstractions.json.linq.RavenJToken;
import net.ravendb.abstractions.util.TimeUtils;
import net.ravendb.client.changes.IDatabaseChanges;
import net.ravendb.client.changes.IObserver;
import net.ravendb.client.connection.ServerClient;
import net.ravendb.client.connection.implementation.HttpJsonRequest;
import net.ravendb.client.extensions.HttpJsonRequestExtension;
import net.ravendb.client.utils.CancellationTokenSource;
import net.ravendb.client.utils.CancellationTokenSource.CancellationToken;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;

import de.undercouch.bson4jackson.BsonFactory;
import de.undercouch.bson4jackson.BsonGenerator;
import org.apache.http.client.methods.CloseableHttpResponse;


public class RemoteBulkInsertOperation implements ILowLevelBulkInsertOperation, IObserver<BulkInsertChangeNotification> {

  private final BsonFactory bsonFactory = new BsonFactory();

  private final static RavenJObject END_OF_QUEUE_OBJECT = RavenJObject.parse("{ \"QueueFinished\" : true }");

  private final BulkInsertOptions options;

  private CancellationTokenSource cancellationTokenSource;
  private final ServerClient operationClient;

  private final ByteArrayOutputStream bufferedStream = new ByteArrayOutputStream();
  private final BlockingQueue<RavenJObject> queue;

  private static final RavenJObject ABORT_MARKER = new RavenJObject();
  private static final RavenJObject SKIP_MARKER = new RavenJObject();

  @SuppressWarnings("unused")
  private HttpJsonRequest operationRequest;
  @SuppressWarnings("unused")
  private byte[] responseBytes;
  private final Thread operationTask;
  private Exception operationTaskException;
  private int total;
  private boolean aborted;

  private static final int BIG_DOCUMENT_SIZE = 64 * 1024;

  private Action1<String> report;
  private long responseOperationId;
  private UUID operationId;
  private transient boolean disposed;

  @Override
  public UUID getOperationId() {
    return operationId;
  }

  @Override
  public Action1<String> getReport() {
    return report;
  }

  @Override
  public void setReport(Action1<String> report) {
    this.report = report;
  }

  public RemoteBulkInsertOperation(BulkInsertOptions options, ServerClient client, IDatabaseChanges changes) {
    this(options, client, changes, null);
  }

  public RemoteBulkInsertOperation(BulkInsertOptions options, ServerClient client, IDatabaseChanges changes, UUID existingOperationId) {
    this.options = options;
    operationId = existingOperationId != null ? existingOperationId : UUID.randomUUID();
    operationClient = client;
    queue = new ArrayBlockingQueue<>(Math.max(128, (options.getBatchSize() * 3) / 2));

    operationTask = startBulkInsertAsync(options);
    subscribeToBulkInsertNotifications(changes);
  }

  private void subscribeToBulkInsertNotifications(IDatabaseChanges changes) {
     changes.forBulkInsert(operationId).subscribe(this);
  }

  public class BulkInsertEntity implements HttpEntity {

    @SuppressWarnings("hiding")
    private BulkInsertOptions options;
    private CancellationToken cancellationToken;

    public BulkInsertEntity(BulkInsertOptions options, CancellationToken cancellationToken) {
      this.options = options;
      this.cancellationToken = cancellationToken;
    }

    @Override
    public boolean isRepeatable() {
      return false;
    }

    @Override
    public boolean isChunked() {
      return true;
    }

    @Override
    public long getContentLength() {
      return 0;
    }

    @Override
    public Header getContentType() {
      return null;
    }

    @Override
    public Header getContentEncoding() {
      return null;
    }

    @Override
    public InputStream getContent() throws IOException, IllegalStateException {
      throw new IllegalStateException("Not supported!");
    }

    @SuppressWarnings("synthetic-access")
    @Override
    public void writeTo(OutputStream outstream) throws IOException {
      writeQueueToServer(outstream, options, cancellationToken);
    }

    @Override
    public boolean isStreaming() {
      return true;
    }

    @Deprecated
    @Override
    public void consumeContent() throws IOException {
      //empty
    }

  }

  private CancellationToken createCancellationToken() {
    cancellationTokenSource = new CancellationTokenSource();
    return cancellationTokenSource.getToken();
  }

  @SuppressWarnings("hiding")
  private Thread startBulkInsertAsync(final BulkInsertOptions options) {
    operationClient.setExpect100Continue(true);

    final String operationUrl = createOperationUrl(options);
    String token;
    try {
      token = getToken();
    } catch (Exception e) {
      queue.add(END_OF_QUEUE_OBJECT);
      throw new IllegalStateException("Could not get token for bulk insert", e);
    }

    try {
      token = validateThatWeCanUseAuthenticateTokens(token);
    } catch (Exception e) {
      queue.add(END_OF_QUEUE_OBJECT);
      throw new IllegalStateException("Could not authenticate token for bulk insert, if you are using ravendb in IIS make sure you have Anonymous Authentication enabled in the IIS configuration", e);
    }
    final String tokenToPass = token;

    Thread thread = new Thread(new Runnable() {

      @SuppressWarnings({"synthetic-access", "boxing"})
      @Override
      public void run() {
        try (HttpJsonRequest operationRequest = createOperationRequest(operationUrl, tokenToPass)) {
          CancellationToken cancellationToken = createCancellationToken();
          CloseableHttpResponse response = operationRequest.executeRawRequest(new BulkInsertEntity(options, cancellationToken));

          HttpJsonRequestExtension.assertNotFailingResponse(response);
          long operationId;

          try {
            String stream = IOUtils.toString(response.getEntity().getContent(), "UTF-8");
            RavenJObject result = RavenJObject.parse(stream);
            operationId = result.value(Long.class, "OperationId");

            if (isOperationCompleted(operationId)) {
              responseOperationId = operationId;
            }

          } catch (IOException e) {
            throw new RuntimeException(e);
          }

        } finally {
          operationClient.setExpect100Continue(false);
        }
      }
    });


    thread.setUncaughtExceptionHandler(new UncaughtExceptionHandler() {
      @SuppressWarnings("synthetic-access")
      @Override
      public void uncaughtException(Thread t, Throwable e) {
        operationTaskException = (Exception) e;
      }
    });

    thread.start();
    return thread;

  }

  private String getToken() {
    RavenJToken jsonToken = getAuthToken();
    return jsonToken.value(String.class, "Token");
  }

  private RavenJToken getAuthToken() {
    try (HttpJsonRequest request = operationClient.createRequest(HttpMethods.GET, "/singleAuthToken", true, false, null)) {
      return request.readResponseJson();
    }
  }

  private String validateThatWeCanUseAuthenticateTokens(String token) {
    try (HttpJsonRequest request = operationClient.createRequest(HttpMethods.GET, "/singleAuthToken", true, true, null)) {
      request.removeAuthorizationHeader();
      request.addOperationHeader("Single-Use-Auth-Token", token);
      RavenJToken result = request.readResponseJson();
      return result.value(String.class, "Token");
    }
  }

  @SuppressWarnings("boxing")
  private HttpJsonRequest createOperationRequest(String operationUrl, String token) {
    HttpJsonRequest request = operationClient.createRequest(HttpMethods.POST, operationUrl, true, true, 6 * 3600 * 1000L);
    request.addOperationHeader("Single-Use-Auth-Token", token);
    return request;
  }

  @SuppressWarnings("hiding")
  private String createOperationUrl(BulkInsertOptions options) {
    String requestUrl = "/bulkInsert?";
    if (options.isOverwriteExisting()) {
      requestUrl += "overwriteExisting=true";
    }
    if (options.isCheckReferencesInIndexes()) {
      requestUrl += "&checkReferencesInIndexes=true";
    }
    if (options.isSkipOverwriteIfUnchanged()) {
      requestUrl += "&skipOverwriteIfUnchanged=true";
    }

    requestUrl += "&operationId=" + operationId;

    return requestUrl;
  }

  @SuppressWarnings("hiding")
  private void writeQueueToServer(OutputStream stream, BulkInsertOptions options, CancellationToken cancellationToken) throws IOException {
    while (true) {
      cancellationToken.throwIfCancellationRequested();
      List<RavenJObject> batch = new ArrayList<>();
      try {
        RavenJObject document;
        while ((document = queue.poll(200, TimeUnit.MICROSECONDS)) != null) {
          cancellationToken.throwIfCancellationRequested();

          if (document == END_OF_QUEUE_OBJECT) { //marker
            flushBatch(stream, batch);
            return;
          }
          if (document == SKIP_MARKER) { // ignore this, just filling the queue
            continue;
          }
          if (document == ABORT_MARKER) { // abort immediately
            return;
          }
          batch.add(document);

          if (batch.size() >= options.getBatchSize()) {
            break;
          }
        }
      } catch (InterruptedException e ){
        //ignore
      }
      flushBatch(stream, batch);
    }
  }

  @Override
  public void write(String id, RavenJObject metadata, RavenJObject data) throws InterruptedException {
    write(id, metadata, data, null);
  }

  @SuppressWarnings("boxing")
  @Override
  public void write(String id, RavenJObject metadata, RavenJObject data, Integer dataSize) throws InterruptedException {
    if (id == null) {
      throw new IllegalArgumentException("id");
    }
    if (metadata == null) {
      throw new IllegalArgumentException("metadata");
    }
    if (data == null) {
      throw new IllegalArgumentException("data");
    }
    if (aborted) {
      throw new IllegalStateException("Operation has been aborted");
    }

    metadata.add("@id", id);
    data.add(Constants.METADATA, metadata);
    for (int i = 0; i < 2; i++) {
      if (operationTask.isInterrupted() || !operationTask.isAlive()){
        operationTask.join();
        if (operationTaskException != null) {
          throw new InterruptedException("Bulk insert timeouted or was aborted");
        }
      }

      if (queue.offer(data, options.getWriteTimeoutMiliseconds() / 2, TimeUnit.MILLISECONDS)) {
        if (dataSize != null && dataSize >= BIG_DOCUMENT_SIZE) {
          //essentially for a BatchSize == 1024 and stream of 1MB documents - the actual batch size will be 128
          // --> BatchSize = 1024 / (dataSize = 1024/BigDocumentSize = 250) * 2 == 128
          for (int skipDocIndex = 0; skipDocIndex < (dataSize / BIG_DOCUMENT_SIZE) * 2; skipDocIndex++) {
            if (!queue.offer(SKIP_MARKER)) {
              break;
            }
          }
        }
        return;
      }
    }

    if (operationTask.isInterrupted() || !operationTask.isAlive()){
      operationTask.join();
      if (operationTaskException != null) {
        throw new InterruptedException("Bulk insert was timeouted or aborted");
      }
    }

    throw new IllegalStateException("Could not flush in the specified timeout, server probably not responding or responding too slowly.\r\nAre you writing very big documents?");

  }


  @SuppressWarnings({"hiding", "boxing"})
  private boolean isOperationCompleted(long operationId) {
    RavenJToken status = getOperationStatus(operationId);
    if (status == null) {
      return true;
    }
    if (status.value(Boolean.class, "Completed")) {
      return true;
    }
    return false;
  }

  @SuppressWarnings("hiding")
  private RavenJToken getOperationStatus(long operationId) {
    return operationClient.getOperationStatus(operationId);
  }


  @Override
  public void close() {
    if (disposed) {
      return ;
    }
    queue.add(END_OF_QUEUE_OBJECT);
    try {
      operationTask.join();
    } catch (InterruptedException e) {
      throw new IllegalStateException(e);
    }

    if (operationTaskException != null) {
      throw new RuntimeException(operationTaskException);
    }

    reportInternal("Finished writing all results to server");

    while (true) {
      if (isOperationCompleted(responseOperationId)) {
        break;
      }
      TimeUtils.cleanSleep(500);
    }
    reportInternal("Done writing to server");
  }

  @SuppressWarnings({"hiding", "boxing"})
  private  void flushBatch(OutputStream requestStream, Collection<RavenJObject> localBatch) throws IOException {
    if (localBatch.isEmpty()) {
      return ;
    }
    if (aborted) {
      throw new IllegalStateException("Operation was timed out or has been aborted");
    }
    bufferedStream.reset();
    writeToBuffer(localBatch);

    byte[] bytes = ByteBuffer.allocate(4).putInt(bufferedStream.size()).array();
    ArrayUtils.reverse(bytes);
    requestStream.write(bytes);
    bufferedStream.writeTo(requestStream);
    requestStream.flush();

    total += localBatch.size();

    Action1<String> report = getReport();
    if (report != null) {
      report.apply(String.format("Wrote %d (total %d) documents to server gzipped to %d kb", localBatch.size(), total, bufferedStream.size() / 1024));
    }

  }

  private void writeToBuffer(Collection<RavenJObject> localBatch) throws IOException {
    GZIPOutputStream gzipOutputStream = new GZIPOutputStream(bufferedStream);

    BsonGenerator bsonWriter = bsonFactory.createJsonGenerator(gzipOutputStream);
    bsonWriter.disable(org.codehaus.jackson.JsonGenerator.Feature.AUTO_CLOSE_TARGET);

    byte[] bytes = ByteBuffer.allocate(4).putInt(localBatch.size()).array();
    ArrayUtils.reverse(bytes);
    gzipOutputStream.write(bytes);
    for (RavenJObject doc : localBatch) {
      doc.writeTo(bsonWriter);
    }
    bsonWriter.close();
    gzipOutputStream.finish();
    bufferedStream.flush();
  }

  private void reportInternal(String format, Object... args) {
    Action1<String> onReport = report;
    if (onReport != null) {
      onReport.apply(String.format(format, args));
    }
  }

  @Override
  public void onNext(BulkInsertChangeNotification value) {
    if (value.getType().equals(DocumentChangeTypes.BULK_INSERT_ERROR)) {
      cancellationTokenSource.cancel();
    }
  }

  @Override
  public void onError(Exception error) {
    //empty by design
  }

  @Override
  public void onCompleted() {
    //empty by design
  }

  @Override
  public void abort() {
    aborted = true;
    queue.add(ABORT_MARKER);
  }

  @Override
  public boolean isAborted() {
    return aborted;
  }
}
