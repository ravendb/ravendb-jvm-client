package net.ravendb.client.documents.commands.multiGet;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import net.ravendb.client.extensions.HttpExtensions;
import net.ravendb.client.extensions.JsonExtensions;
import net.ravendb.client.http.*;
import net.ravendb.client.json.ContentProviderHttpEntity;
import net.ravendb.client.primitives.CleanCloseable;
import net.ravendb.client.primitives.Reference;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.ContentType;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MultiGetCommand extends RavenCommand<List<GetResponse>> {

    private final RequestExecutor _requestExecutor;
    private final HttpCache _cache;
    private final List<GetRequest> _commands;

    private String _baseUrl;

    @SuppressWarnings("unchecked")
    public MultiGetCommand(RequestExecutor requestExecutor, List<GetRequest> commands) {
        super((Class<List<GetResponse>>)(Class<?>)List.class);

        if (requestExecutor == null) {
            throw new IllegalArgumentException("RequestExecutor cannot be null");
        }

        if (commands == null) {
            throw new IllegalArgumentException("Command cannot be null");
        }
        _requestExecutor = requestExecutor;
        _cache = requestExecutor.getCache();
        _commands = commands;
        responseType = RavenCommandResponseType.RAW;
    }

    @Override
    public HttpRequestBase createRequest(ServerNode node, Reference<String> url) {
        _baseUrl = node.getUrl() + "/databases/" + node.getDatabase();

        url.value = _baseUrl + "/multi_get";

        AggressiveCacheOptions aggressiveCacheOptions = _requestExecutor.aggressiveCaching.get();
        if (aggressiveCacheOptions != null && aggressiveCacheOptions.getMode() == AggressiveCacheMode.TRACK_CHANGES) {
            result = new ArrayList<>();

            for (GetRequest command : _commands) {
                if (!command.isCanCacheAggressively()) {
                    break;
                }
                String cacheKey = getCacheKey(command, new Reference<>());
                Reference<String> cachedRef = new Reference<>();
                try (HttpCache.ReleaseCacheItem cachedItem = _cache.get(cacheKey, new Reference<>(), cachedRef)) {
                    if (cachedRef.value == null
                            || cachedItem.getAge().compareTo(aggressiveCacheOptions.getDuration()) > 0
                            || cachedItem.getMightHaveBeenModified()) {
                        break;
                    }
                    GetResponse getResponse = new GetResponse();
                    getResponse.setResult(cachedRef.value);
                    getResponse.setStatusCode(HttpStatus.SC_NOT_MODIFIED);
                    result.add(getResponse);
                }
            }

            if (result.size() == _commands.size()) {
                return null; // aggressively cached
            }

            // not all of it is cached, might as well read it all
            result = null;
        }

        HttpPost request = new HttpPost();
        ObjectMapper mapper = JsonExtensions.getDefaultMapper();

        request.setEntity(new ContentProviderHttpEntity(outputStream -> {
            try (JsonGenerator generator = mapper.getFactory().createGenerator(outputStream)) {

                generator.writeStartObject();

                generator.writeFieldName("Requests");
                generator.writeStartArray();

                for (GetRequest command : _commands) {
                    String cacheKey = getCacheKey(command, new Reference<>());

                    Reference<String> cachedChangeVector = new Reference<>();
                    try (CleanCloseable item = _cache.get(cacheKey, cachedChangeVector, new Reference<>())) {
                        Map<String, String> headers = new HashMap<>();
                        if (cachedChangeVector.value != null) {
                            headers.put("If-None-Match", "\"" + cachedChangeVector.value + "\"");
                        }

                        for (Map.Entry<String, String> header : command.getHeaders().entrySet()) {
                            headers.put(header.getKey(), header.getValue());
                        }

                        generator.writeStartObject();

                        generator.writeStringField("Url", "/databases/" + node.getDatabase() + command.getUrl());
                        generator.writeStringField("Query", command.getQuery());

                        generator.writeStringField("Method", command.getMethod());

                        generator.writeFieldName("Headers");
                        generator.writeStartObject();

                        for (Map.Entry<String, String> kvp : headers.entrySet()) {
                            generator.writeStringField(kvp.getKey(), kvp.getValue());
                        }
                        generator.writeEndObject();

                        generator.writeFieldName("Content");
                        if (command.getContent() != null) {
                            command.getContent().writeContent(generator);
                        } else {
                            generator.writeNull();
                        }

                        generator.writeEndObject();
                    }
                }
                generator.writeEndArray();
                generator.writeEndObject();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }, ContentType.APPLICATION_JSON));

        return request;
    }

    private String getCacheKey(GetRequest command, Reference<String> requestUrl) {
        requestUrl.value = _baseUrl + command.getUrlAndQuery();
        return command.getMethod() + "-" + requestUrl.value;
    }

    @Override
    public void setResponseRaw(CloseableHttpResponse response, InputStream stream) {
        try (JsonParser parser = mapper.getFactory().createParser(stream)) {
            if (parser.nextToken() != JsonToken.START_OBJECT) {
                throwInvalidResponse();
            }

            String property = parser.nextFieldName();
            if (!"Results".equals(property)) {
                throwInvalidResponse();
            }

            int i = 0;
            result = new ArrayList<>();

            for (GetResponse getResponse : readResponses(mapper, parser)) {
                GetRequest command = _commands.get(i);
                maybeSetCache(getResponse, command);
                maybeReadFromCache(getResponse, command);

                result.add(getResponse);

                i++;
            }

            if (parser.nextToken() != JsonToken.END_OBJECT) {
                throwInvalidResponse();
            }


        } catch (Exception e) {
            throwInvalidResponse(e);
        }
    }

    @SuppressWarnings("ConditionalBreakInInfiniteLoop")
    private static List<GetResponse> readResponses(ObjectMapper mapper, JsonParser parser) throws IOException {
        if (parser.nextToken() != JsonToken.START_ARRAY) {
            throwInvalidResponse();
        }

        List<GetResponse> responses = new ArrayList<>();

        while (true) {
            if (parser.nextToken() == JsonToken.END_ARRAY) {
                break;
            }

            responses.add(readResponse(mapper, parser));
        }

        return responses;
    }

    private static GetResponse readResponse(ObjectMapper mapper, JsonParser parser) throws IOException {
        if (parser.currentToken() != JsonToken.START_OBJECT) {
            throwInvalidResponse();
        }

        GetResponse getResponse = new GetResponse();

        while (true) {
            if (parser.nextToken() == null) {
                throwInvalidResponse();
            }

            if (parser.currentToken() == JsonToken.END_OBJECT) {
                break;
            }

            if (parser.currentToken() != JsonToken.FIELD_NAME) {
                throwInvalidResponse();
            }

            String property = parser.getValueAsString();
            switch (property) {
                case "Result":
                    JsonToken jsonToken = parser.nextToken();
                    if (jsonToken == null) {
                        throwInvalidResponse();
                    }

                    if (parser.currentToken() == JsonToken.VALUE_NULL) {
                        continue;
                    }

                    if (parser.currentToken() != JsonToken.START_OBJECT) {
                        throwInvalidResponse();
                    }

                    TreeNode treeNode = mapper.readTree(parser);
                    getResponse.setResult(treeNode.toString());
                    continue;
                case "Headers":
                    if (parser.nextToken() == null) {
                        throwInvalidResponse();
                    }

                    if (parser.currentToken() == JsonToken.VALUE_NULL) {
                        continue;
                    }

                    if (parser.currentToken() != JsonToken.START_OBJECT) {
                        throwInvalidResponse();
                    }

                    ObjectNode headersMap = mapper.readTree(parser);
                    headersMap.fieldNames().forEachRemaining(field -> getResponse.getHeaders().put(field, headersMap.get(field).asText()));
                    continue;
                case "StatusCode":
                    int statusCode = parser.nextIntValue(-1);
                    if (statusCode == -1) {
                        throwInvalidResponse();
                    }

                    getResponse.setStatusCode(statusCode);
                    continue;
                default:
                    throwInvalidResponse();
                    break;

            }


        }

        return getResponse;
    }


    private void maybeReadFromCache(GetResponse getResponse, GetRequest command) {
        if (getResponse.getStatusCode() != HttpStatus.SC_NOT_MODIFIED) {
            return;
        }

        String cacheKey = getCacheKey(command, new Reference<>());
        Reference<String> cachedResponse = new Reference<>();
        try (CleanCloseable cacheItem = _cache.get(cacheKey, new Reference<>(), cachedResponse)) {
            getResponse.setResult(cachedResponse.value);
        }
    }

    private void maybeSetCache(GetResponse getResponse, GetRequest command) {
        if (getResponse.getStatusCode() == HttpStatus.SC_NOT_MODIFIED) {
            return;
        }

        String cacheKey = getCacheKey(command, new Reference<>());

        String result = getResponse.getResult();
        if (result == null) {
            return;
        }

        String changeVector = HttpExtensions.getEtagHeader(getResponse.getHeaders());
        if (changeVector == null) {
            return;
        }

        _cache.set(cacheKey, changeVector, result);
    }

    @Override
    public boolean isReadRequest() {
        return false;
    }
}
