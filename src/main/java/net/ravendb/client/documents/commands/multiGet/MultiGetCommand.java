package net.ravendb.client.documents.commands.multiGet;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import net.ravendb.client.Constants;
import net.ravendb.client.documents.session.SessionInfo;
import net.ravendb.client.extensions.HttpExtensions;
import net.ravendb.client.http.*;
import net.ravendb.client.json.ContentProviderHttpEntity;
import net.ravendb.client.primitives.CleanCloseable;
import net.ravendb.client.primitives.Reference;
import net.ravendb.client.primitives.Tuple;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpStatus;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@SuppressWarnings("unchecked")
public class MultiGetCommand extends RavenCommand<List<GetResponse>> implements CleanCloseable {
    private final RequestExecutor _requestExecutor;
    private final HttpCache _httpCache;
    private final List<GetRequest> _commands;
    private final SessionInfo _sessionInfo;

    private String _baseUrl;
    private Cached _cached;

    public boolean aggressivelyCached;

    public MultiGetCommand(RequestExecutor requestExecutor, List<GetRequest> commands) {
        this(requestExecutor, commands, null);
    }

    public MultiGetCommand(RequestExecutor requestExecutor, List<GetRequest> commands, SessionInfo sessionInfo) {
        super((Class<List<GetResponse>>)(Class<?>)List.class);

        if (requestExecutor == null) {
            throw new IllegalArgumentException("RequestExecutor cannot be null");
        }

        HttpCache cache = requestExecutor.getCache();

        if (cache == null) {
            throw new IllegalArgumentException("Cache cannot be null");
        }

        if (commands == null) {
            throw new IllegalArgumentException("Command cannot be null");
        }

        _requestExecutor = requestExecutor;
        _httpCache = requestExecutor.getCache();
        _commands = commands;
        _sessionInfo = sessionInfo;
        responseType = RavenCommandResponseType.RAW;
    }

    @Override
    public HttpUriRequestBase createRequest(ServerNode node) {
        _baseUrl = node.getUrl() + "/databases/" + node.getDatabase();
        String url = _baseUrl + "/multi_get";

        if ((_sessionInfo == null || !_sessionInfo.isNoCaching()) && maybeReadAllFromCache(_requestExecutor.aggressiveCaching.get()))
        {
            aggressivelyCached = true;
            return null; // aggressively cached
        }

        HttpPost request = new HttpPost(url);

        request.setEntity(new ContentProviderHttpEntity(outputStream -> {
            try (JsonGenerator generator = createSafeJsonGenerator(outputStream)) {
                generator.writeStartObject();

                generator.writeFieldName("Requests");
                generator.writeStartArray();

                for (GetRequest command : _commands) {
                    generator.writeStartObject();

                    generator.writeStringField("Url", "/databases/" + node.getDatabase() + command.getUrl());
                    generator.writeStringField("Query", command.getQuery());

                    generator.writeStringField("Method", command.getMethod());

                    generator.writeFieldName("Headers");
                    generator.writeStartObject();

                    for (Map.Entry<String, String> kvp : command.getHeaders().entrySet()) {
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
                generator.writeEndArray();
                generator.writeEndObject();
            }
        }, ContentType.APPLICATION_JSON, _requestExecutor.getConventions()));

        return request;
    }

    private boolean maybeReadAllFromCache(AggressiveCacheOptions options) {
        closeCache();

        boolean readAllFromCache = options != null;
        boolean trackChanges = readAllFromCache && options.getMode() == AggressiveCacheMode.TRACK_CHANGES;

        for (int i = 0; i < _commands.size(); i++) {
            GetRequest command = _commands.get(i);

            if (command.getHeaders().containsKey(Constants.Headers.IF_NONE_MATCH)) {
                continue; // command already explicitly handling setting this, let's not touch it.
            }

            String cacheKey = getCacheKey(command, new Reference<>());

            Reference<String> changeVectorRef = new Reference<>();
            Reference<String> cachedRef = new Reference<>();

            HttpCache.ReleaseCacheItem cachedItem = _httpCache.get(cacheKey, changeVectorRef, cachedRef);
            if (cachedItem.item == null) {
                try {
                    readAllFromCache = false;
                    continue;
                } finally {
                    cachedItem.close();
                }
            }

            if (readAllFromCache && (trackChanges && cachedItem.getMightHaveBeenModified() || cachedItem.getAge().compareTo(options.getDuration()) > 0) || !command.isCanCacheAggressively()) {
                readAllFromCache = false;
            }

            command.getHeaders().put(Constants.Headers.IF_NONE_MATCH, changeVectorRef.value);
            if (_cached == null) {
                _cached = new Cached(_commands.size());
            }

            _cached.values[i] = Tuple.create(cachedItem, cachedRef.value);
        }

        if (readAllFromCache) {
            try (CleanCloseable context = _cached) {
                result = new ArrayList<>(_commands.size());

                for (int i = 0; i < _commands.size(); i++) {
                    Tuple<HttpCache.ReleaseCacheItem, String> itemAndCached = _cached.values[i];
                    GetResponse getResponse = new GetResponse();
                    getResponse.setResult(itemAndCached.second);
                    getResponse.setStatusCode(HttpStatus.SC_NOT_MODIFIED);

                    result.add(getResponse);
                }
            }

            _cached = null;
        }

        return readAllFromCache;
    }

    private String getCacheKey(GetRequest command, Reference<String> requestUrl) {
        requestUrl.value = _baseUrl + command.getUrlAndQuery();
        return command.getMethod() != null ? command.getMethod() + "-" + requestUrl.value : requestUrl.value;
    }

    @Override
    public void setResponseRaw(ClassicHttpResponse response, InputStream stream) {
        try (JsonParser parser = mapper.getFactory().createParser(stream)) {
            try {
                if (parser.nextToken() != JsonToken.START_OBJECT) {
                    throwInvalidResponse();
                }

                String property = parser.nextFieldName();
                if (!"Results".equals(property)) {
                    throwInvalidResponse();
                }

                int i = 0;
                result = new ArrayList<>(_commands.size());

                for (GetResponse getResponse : readResponses(mapper, parser)) {
                    GetRequest command = _commands.get(i);
                    maybeSetCache(getResponse, command, i);

                    if (_cached != null && getResponse.getStatusCode() == HttpStatus.SC_NOT_MODIFIED) {
                        GetResponse clonedResponse = new GetResponse();
                        clonedResponse.setResult(_cached.values[i].second);
                        clonedResponse.setStatusCode(HttpStatus.SC_NOT_MODIFIED);
                        result.add(clonedResponse);
                    } else {
                        result.add(getResponse);
                    }

                    i++;
                }

                if (parser.nextToken() != JsonToken.END_OBJECT) {
                    throwInvalidResponse();
                }

            } finally {
                if (_cached != null) {
                    _cached.close();
                }
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

    private void maybeSetCache(GetResponse getResponse, GetRequest command, int cachedIndex) {
        if (getResponse.getStatusCode() == HttpStatus.SC_NOT_MODIFIED) {
            // if not modified - update age
            if (_cached != null) {
                _cached.values[cachedIndex].first.notModified();
            }
            return;
        }

        String cacheKey = getCacheKey(command, new Reference<>());

        String result = getResponse.getResult();
        if (result == null) {
            _httpCache.setNotFound(cacheKey, aggressivelyCached);
            return;
        }

        String changeVector = HttpExtensions.getEtagHeader(getResponse.getHeaders());
        if (changeVector == null) {
            return;
        }

        _httpCache.set(cacheKey, changeVector, result);
    }

    @Override
    public boolean isReadRequest() {
        return false;
    }

    @Override
    public void close() {
        closeCache();
    }

    public void closeCache() {
        //If _cached is not null - it means that the client approached with this multitask request to node and the request failed.
        //and now client tries to send it to another node.
        if (_cached != null) {
            _cached.close();

            _cached = null;

            // The client sends the commands.
            // Some of which could be saved in cache with a response
            // that includes the change vector that received from the old fallen node.
            // The client can't use those responses because their URLs are different
            // (include the IP and port of the old node), because of that the client
            // needs to get those docs again from the new node.

            for (GetRequest command : _commands) {
                command.getHeaders().remove(Constants.Headers.IF_NONE_MATCH);
            }
        }
    }

    private static class Cached implements CleanCloseable {
        private final int _size;

        public Tuple<HttpCache.ReleaseCacheItem, String>[] values;


        public Cached(int size) {
            _size = size;
            values = new Tuple[size];
        }

        @Override
        public void close() {
            if (values == null) {
                return;
            }

            for (int i = 0; i < _size; i++) {
                if (values[i] != null) {
                    values[i].first.close();
                }
            }

            values = null;
        }
    }
}
