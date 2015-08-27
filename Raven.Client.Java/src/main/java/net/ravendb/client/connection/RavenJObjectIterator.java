package net.ravendb.client.connection;

import net.ravendb.abstractions.basic.CloseableIterator;
import net.ravendb.abstractions.closure.Function1;
import net.ravendb.abstractions.exceptions.JsonReaderException;
import net.ravendb.abstractions.json.linq.RavenJObject;
import net.ravendb.client.RavenPagingInformation;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.util.EntityUtils;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonToken;

import java.io.IOException;
import java.io.InputStream;

public class RavenJObjectIterator implements CloseableIterator<RavenJObject> {

  private HttpEntity httpEntity;
  private CloseableHttpResponse httpResponse;
  private JsonParser jsonParser;
  private InputStream stream;
  private boolean hasNext;
  private RavenJObject currentObject;
  private int start;
  private int pageSize;
  private RavenPagingInformation pagingInformation;
  private boolean wasInitialized;
  private boolean complete;
  private Function1<JsonParser, Boolean> customizedEndResult;

  public RavenJObjectIterator(CloseableHttpResponse response, int start, int pageSize,
    RavenPagingInformation pagingInformation, Function1<JsonParser, Boolean> customizedEndResult) {
    try {
      this.httpResponse = response;
      httpEntity = httpResponse.getEntity();
      this.stream = httpEntity.getContent();
      jsonParser = new JsonFactory().createJsonParser(stream);
      this.start = start;
      this.pageSize = pageSize;
      this.pagingInformation = pagingInformation;
      this.customizedEndResult = customizedEndResult;

      fetchNextObject();
    } catch (IOException e) {
      throw new JsonReaderException(e);
    }
  }

  private void fetchNextObject() throws JsonParseException, IOException {
    if (complete) {
      return;
    }
    if (wasInitialized == false) {
      init();
      wasInitialized = true;
    }

    JsonToken token = jsonParser.nextToken();
    if (token == JsonToken.END_ARRAY) {
      hasNext = false;
      complete = true;

      tryReadNextPageStart();
      ensureValidEndOfResponse();

      EntityUtils.consumeQuietly(httpEntity);
      this.currentObject = null;
    } else {
      this.currentObject = RavenJObject.load(jsonParser);
      this.hasNext = true;
    }
  }

  @SuppressWarnings("boxing")
  private void tryReadNextPageStart() throws IOException {
    if (jsonParser.nextToken() == null || jsonParser.getCurrentToken() != JsonToken.FIELD_NAME) {
      return;
    }
    String propName = jsonParser.getText();

    switch (propName) {
      case  "NextPageStart":
        jsonParser.nextToken();
        int nextPageStart = jsonParser.getIntValue();
        if (pagingInformation == null) {
          return;
        }
        pagingInformation.fill(start, pageSize, nextPageStart);
        break;
      case "Error":
        jsonParser.nextToken();
        String error = jsonParser.getText();
        throw new IllegalStateException("Server error\n" + error);
      default:
          if (customizedEndResult != null && customizedEndResult.apply(jsonParser))
            break;
          throw new IllegalStateException("Unexpected property name: " + jsonParser.getText());
    }
  }

  private void ensureValidEndOfResponse() throws IOException {
    if (jsonParser.getCurrentToken() != JsonToken.END_OBJECT && jsonParser.nextToken() == null) {
      throw new IllegalStateException("Unexpected end of response - missing EndObject token");
    }

    if (jsonParser.getCurrentToken() != JsonToken.END_OBJECT) {
      throw new IllegalStateException("Unexpected token type at the end of the response: " + jsonParser.getCurrentToken() + ". Error: " + IOUtils.toString(stream, "UTF-8"));
    }

    String remaingingContent = IOUtils.toString(stream, "UTF-8");

    if (StringUtils.isNotEmpty(remaingingContent)) {
      throw new IllegalStateException("Server error: " + remaingingContent);
    }
  }

  private void init() throws IOException {
    if (jsonParser.nextToken() == null || jsonParser.getCurrentToken() != JsonToken.START_OBJECT) {
      throw new IllegalStateException("Unexpected data at start of stream");
    }
    if (jsonParser.nextToken() == null || jsonParser.getCurrentToken() != JsonToken.FIELD_NAME || !"Results".equals(jsonParser.getText())) {
      throw new IllegalStateException("Unexpected data at stream 'Results' property name");
    }
    if (jsonParser.nextToken() == null || jsonParser.getCurrentToken() != JsonToken.START_ARRAY) {
      throw new IllegalStateException("Unexpected data at 'Results', could not find start results array");
    }
  }

  @Override
  public boolean hasNext() {
    return hasNext;
  }

  @Override
  public RavenJObject next() {
    RavenJObject current = currentObject;
    try {
      fetchNextObject();
    } catch (IOException e) {
      throw new JsonReaderException("Unable to read object");
    }
    return current;
  }

  @Override
  public void remove() {
    throw new IllegalStateException("You can't remove entries");
  }

  @Override
  public void close() {
    EntityUtils.consumeQuietly(httpEntity);
  }

}
