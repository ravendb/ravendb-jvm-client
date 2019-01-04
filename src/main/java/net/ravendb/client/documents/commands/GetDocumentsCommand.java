package net.ravendb.client.documents.commands;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.ravendb.client.Constants;
import net.ravendb.client.documents.queries.HashCalculator;
import net.ravendb.client.extensions.JsonExtensions;
import net.ravendb.client.http.RavenCommand;
import net.ravendb.client.http.ServerNode;
import net.ravendb.client.json.ContentProviderHttpEntity;
import net.ravendb.client.primitives.Reference;
import net.ravendb.client.util.UrlUtils;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.ContentType;

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

public class GetDocumentsCommand extends RavenCommand<GetDocumentsResult> {

    private String _id;

    private String[] _ids;
    private String[] _includes;
    private String[] _counters;
    private boolean _includeAllCounters;

    private boolean _metadataOnly;

    private String _startWith;
    private String _matches;
    private Integer _start;
    private Integer _pageSize;
    private String _exclude;
    private String _startAfter;

    public GetDocumentsCommand(int start, int pageSize) {
        super(GetDocumentsResult.class);
        _start = start;
        _pageSize = pageSize;
    }

    public GetDocumentsCommand(String id, String[] includes, boolean metadataOnly) {
        super(GetDocumentsResult.class);
        if (id == null) {
            throw new IllegalArgumentException("id cannot be null");
        }
        _id = id;
        _includes = includes;
        _metadataOnly = metadataOnly;
    }

    public GetDocumentsCommand(String[] ids, String[] includes, boolean metadataOnly) {
        super(GetDocumentsResult.class);
        if (ids == null || ids.length == 0) {
            throw new IllegalArgumentException("Please supply at least one id");
        }

        _ids = ids;
        _includes = includes;
        _metadataOnly = metadataOnly;
    }

    public GetDocumentsCommand(String[] ids, String[] includes, String[] counterIncludes, boolean metadataOnly) {
        this(ids, includes, metadataOnly);

        if (counterIncludes == null) {
            throw new IllegalArgumentException("CounterIncludes cannot be null");
        }

        _counters = counterIncludes;
    }

    public GetDocumentsCommand(String[] ids, String[] includes, boolean includeAllCounters, boolean metadataOnly) {
        this(ids, includes, metadataOnly);
        _includeAllCounters = includeAllCounters;
    }

    public GetDocumentsCommand(String startWith, String startAfter, String matches, String exclude, int start, int pageSize, boolean metadataOnly) {
        super(GetDocumentsResult.class);
        if (startWith == null) {
            throw new IllegalArgumentException("startWith cannot be null");
        }
        _startWith = startWith;
        _startAfter = startAfter;
        _matches = matches;
        _exclude = exclude;
        _start = start;
        _pageSize = pageSize;
        _metadataOnly = metadataOnly;
    }

    @Override
    public HttpRequestBase createRequest(ServerNode node, Reference<String> url) {
        StringBuilder pathBuilder = new StringBuilder(node.getUrl());
        pathBuilder.append("/databases/")
                .append(node.getDatabase())
                .append("/docs?");

        if (_start != null) {
            pathBuilder.append("&start=").append(_start);
        }

        if (_pageSize != null) {
            pathBuilder.append("&pageSize=").append(_pageSize);
        }

        if (_metadataOnly) {
            pathBuilder.append("&metadataOnly=true");
        }

        if (_startWith != null) {
            pathBuilder.append("&startsWith=");
            pathBuilder.append(UrlUtils.escapeDataString(_startWith));

            if (_matches != null) {
                pathBuilder.append("&matches=");
                pathBuilder.append(_matches);
            }

            if (_exclude != null) {
                pathBuilder.append("&exclude=");
                pathBuilder.append(_exclude);
            }

            if (_startAfter != null) {
                pathBuilder.append("&startAfter=");
                pathBuilder.append(_startAfter);
            }
        }

        if (_includes != null) {
            for (String include : _includes) {
                pathBuilder.append("&include=");
                pathBuilder.append(include);
            }
        }

        if (_includeAllCounters) {
            pathBuilder
                    .append("&counter=")
                    .append(Constants.Counters.ALL);
        } else if (_counters != null && _counters.length > 0) {
            for (String counter : _counters) {
                pathBuilder.append("&counter=").append(counter);
            }
        }

        HttpRequestBase request = new HttpGet();

        if (_id != null) {
            pathBuilder.append("&id=");
            pathBuilder.append(UrlUtils.escapeDataString(_id));
        } else if (_ids != null) {
            request = prepareRequestWithMultipleIds(pathBuilder, request, _ids);
        }

        url.value = pathBuilder.toString();
        return request;
    }

    public static HttpRequestBase prepareRequestWithMultipleIds(StringBuilder pathBuilder, HttpRequestBase request, String[] ids) {
        Set<String> uniqueIds = new LinkedHashSet<>();
        Collections.addAll(uniqueIds, ids);

        // if it is too big, we drop to POST (note that means that we can't use the HTTP cache any longer)
        // we are fine with that, requests to load > 1024 items are going to be rare
        boolean isGet = uniqueIds.stream()
                .map(String::length)
                .reduce((prev, current) -> prev + current)
                .orElse(0) < 1024;

        if (isGet) {
            uniqueIds.forEach(x -> {
                if (x != null) {
                    pathBuilder.append("&id=");
                    pathBuilder.append(UrlUtils.escapeDataString(x));
                }
            });

            return new HttpGet();
        } else {
            HttpPost httpPost = new HttpPost();

            try {
                String calculateHash = calculateHash(uniqueIds);
                pathBuilder.append("&loadHash=");
                pathBuilder.append(calculateHash);
            } catch (IOException e) {
                throw new RuntimeException("Unable to compute query hash:" + e.getMessage(), e);
            }

            ObjectMapper mapper = JsonExtensions.getDefaultMapper();

            httpPost.setEntity(new ContentProviderHttpEntity(outputStream -> {
                try (JsonGenerator generator = mapper.getFactory().createGenerator(outputStream)) {
                    generator.writeStartObject();
                    generator.writeFieldName("Ids");
                    generator.writeStartArray();

                    for (String id : uniqueIds) {
                        generator.writeString(id);
                    }

                    generator.writeEndArray();
                    generator.writeEndObject();

                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }, ContentType.APPLICATION_JSON));

            return httpPost;
        }
    }

    private static String calculateHash(Set<String> uniqueIds) throws IOException {
        HashCalculator hasher = new HashCalculator();

        for (String x : uniqueIds) {
            hasher.write(x);
        }

        return hasher.getHash();
    }

    @Override
    public void setResponse(String response, boolean fromCache) throws IOException {
        if (response == null) {
            result = null;
            return;
        }

        result = mapper.readValue(response, resultClass);
    }

    @Override
    public boolean isReadRequest() {
        return true;
    }
}
