package net.ravendb.client.documents.commands;

import com.fasterxml.jackson.core.JsonGenerator;
import net.ravendb.client.Constants;
import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.operations.timeSeries.AbstractTimeSeriesRange;
import net.ravendb.client.documents.operations.timeSeries.TimeSeriesCountRange;
import net.ravendb.client.documents.operations.timeSeries.TimeSeriesRange;
import net.ravendb.client.documents.operations.timeSeries.TimeSeriesTimeRange;
import net.ravendb.client.documents.queries.HashCalculator;
import net.ravendb.client.documents.session.TransactionMode;
import net.ravendb.client.extensions.JsonExtensions;
import net.ravendb.client.http.RavenCommand;
import net.ravendb.client.http.ServerNode;
import net.ravendb.client.json.ContentProviderHttpEntity;
import net.ravendb.client.primitives.NetISO8601Utils;
import net.ravendb.client.primitives.Reference;
import net.ravendb.client.primitives.SharpEnum;
import net.ravendb.client.util.UrlUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.ContentType;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class GetDocumentsCommand extends RavenCommand<GetDocumentsResult> {

    private final DocumentConventions _conventions;
    private String _id;

    private String[] _ids;
    private String[] _includes;
    private String[] _counters;
    private boolean _includeAllCounters;
    private TransactionMode _txMode;

    private List<AbstractTimeSeriesRange> _timeSeriesIncludes;
    private String[] _revisionsIncludeByChangeVector;
    private Date _revisionsIncludeByDateTime;
    private String[] _compareExchangeValueIncludes;

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
        _conventions = null;
    }

    public GetDocumentsCommand(DocumentConventions conventions, String id, String[] includes, boolean metadataOnly) {
        super(GetDocumentsResult.class);
        if (id == null) {
            throw new IllegalArgumentException("id cannot be null");
        }

        _conventions = conventions;
        _id = id;
        _includes = includes;
        _metadataOnly = metadataOnly;
    }

    public GetDocumentsCommand(DocumentConventions conventions, String[] ids, String[] includes, boolean metadataOnly) {
        super(GetDocumentsResult.class);
        if (ids == null || ids.length == 0) {
            throw new IllegalArgumentException("Please supply at least one id");
        }

        _conventions = conventions;
        _ids = ids;
        _includes = includes;
        _metadataOnly = metadataOnly;
    }

    public GetDocumentsCommand(DocumentConventions conventions, String[] ids, String[] includes, String[] counterIncludes,
                               List<AbstractTimeSeriesRange> timeSeriesIncludes, String[] compareExchangeValueIncludes,
                               boolean metadataOnly) {
        this(conventions, ids, includes, metadataOnly);

        _counters = counterIncludes;
        _timeSeriesIncludes = timeSeriesIncludes;
        _compareExchangeValueIncludes = compareExchangeValueIncludes;
    }

    public GetDocumentsCommand(DocumentConventions conventions, String[] ids, String[] includes, String[] counterIncludes,
                               String[] revisionsIncludesByChangeVector, Date revisionIncludeByDateTimeBefore,
                               List<AbstractTimeSeriesRange> timeSeriesIncludes, String[] compareExchangeValueIncludes,
                               boolean metadataOnly) {
        this(conventions, ids, includes, metadataOnly);

        _counters = counterIncludes;
        _revisionsIncludeByChangeVector = revisionsIncludesByChangeVector;
        _revisionsIncludeByDateTime = revisionIncludeByDateTimeBefore;
        _timeSeriesIncludes = timeSeriesIncludes;
        _compareExchangeValueIncludes = compareExchangeValueIncludes;
    }

    public GetDocumentsCommand(DocumentConventions conventions, String[] ids, String[] includes, boolean includeAllCounters,
                               List<AbstractTimeSeriesRange> timeSeriesIncludes, String[] compareExchangeValueIncludes,
                               boolean metadataOnly) {
        this(conventions, ids, includes, metadataOnly);

        _includeAllCounters = includeAllCounters;
        _timeSeriesIncludes = timeSeriesIncludes;
        _compareExchangeValueIncludes = compareExchangeValueIncludes;
    }

    public GetDocumentsCommand(DocumentConventions conventions, String startWith, String startAfter, String matches, String exclude, int start, int pageSize, boolean metadataOnly) {
        super(GetDocumentsResult.class);
        if (startWith == null) {
            throw new IllegalArgumentException("startWith cannot be null");
        }

        _conventions = conventions;
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

        if (_txMode == TransactionMode.CLUSTER_WIDE) {
            pathBuilder.append("&txMode=ClusterWide");
        }

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
                pathBuilder.append(urlEncode(_matches));
            }

            if (_exclude != null) {
                pathBuilder.append("&exclude=");
                pathBuilder.append(urlEncode(_exclude));
            }

            if (_startAfter != null) {
                pathBuilder.append("&startAfter=");
                pathBuilder.append(_startAfter);
            }
        }

        if (_includes != null) {
            for (String include : _includes) {
                pathBuilder.append("&include=");
                pathBuilder.append(urlEncode(include));
            }
        }

        if (_includeAllCounters) {
            pathBuilder
                    .append("&counter=")
                    .append(urlEncode(Constants.Counters.ALL));
        } else if (_counters != null && _counters.length > 0) {
            for (String counter : _counters) {
                pathBuilder.append("&counter=").append(urlEncode(counter));
            }
        }

        if (_timeSeriesIncludes != null) {
            for (AbstractTimeSeriesRange tsInclude : _timeSeriesIncludes) {
                if (tsInclude instanceof TimeSeriesRange) {
                    TimeSeriesRange range = (TimeSeriesRange) tsInclude;
                    pathBuilder.append("&timeseries=")
                            .append(urlEncode(range.getName()))
                            .append("&from=")
                            .append(range.getFrom() != null ? NetISO8601Utils.format(range.getFrom(), true) : "")
                            .append("&to=")
                            .append(range.getTo() != null ? NetISO8601Utils.format(range.getTo(), true) : "");
                } else if (tsInclude instanceof TimeSeriesTimeRange) {
                    TimeSeriesTimeRange timeRange = (TimeSeriesTimeRange) tsInclude;
                    pathBuilder
                            .append("&timeseriestime=")
                            .append(urlEncode(timeRange.getName()))
                            .append("&timeType=")
                            .append(urlEncode(SharpEnum.value(timeRange.getType())))
                            .append("&timeValue=")
                            .append(timeRange.getTime().getValue())
                            .append("&timeUnit=")
                            .append(urlEncode(SharpEnum.value(timeRange.getTime().getUnit())));
                } else if (tsInclude instanceof TimeSeriesCountRange) {
                    TimeSeriesCountRange countRange = (TimeSeriesCountRange) tsInclude;
                    pathBuilder
                            .append("&timeseriescount=")
                            .append(urlEncode(countRange.getName()))
                            .append("&countType=")
                            .append(urlEncode(SharpEnum.value(countRange.getType())))
                            .append("&countValue=")
                            .append(countRange.getCount());
                } else {
                    throw new IllegalArgumentException("Unexpected TimeSeries range " + tsInclude.getClass());
                }
            }
        }

        if (_revisionsIncludeByChangeVector != null) {
            for (String changeVector : _revisionsIncludeByChangeVector) {
                pathBuilder
                        .append("&revisions=")
                        .append(UrlUtils.escapeDataString(changeVector));
            }
        }

        if (_revisionsIncludeByDateTime != null) {
            pathBuilder
                    .append("&revisionsBefore=")
                    .append(UrlUtils.escapeDataString(NetISO8601Utils.format(_revisionsIncludeByDateTime, true)));
        }

        if (_compareExchangeValueIncludes != null) {
            for (String compareExchangeValue : _compareExchangeValueIncludes) {
                pathBuilder
                        .append("&cmpxchg=")
                        .append(urlEncode(compareExchangeValue));
            }
        }

        HttpRequestBase request = new HttpGet();

        if (_id != null) {
            pathBuilder.append("&id=");
            pathBuilder.append(UrlUtils.escapeDataString(_id));
        } else if (_ids != null) {
            request = prepareRequestWithMultipleIds(_conventions, pathBuilder, request, _ids);
        }

        url.value = pathBuilder.toString();
        return request;
    }

    public static HttpRequestBase prepareRequestWithMultipleIds(DocumentConventions conventions, StringBuilder pathBuilder, HttpRequestBase request, String[] ids) {
        Set<String> uniqueIds = new LinkedHashSet<>();
        Collections.addAll(uniqueIds, ids);

        // if it is too big, we drop to POST (note that means that we can't use the HTTP cache any longer)
        // we are fine with that, requests to load > 1024 items are going to be rare
        boolean isGet = uniqueIds.stream()
                .filter(Objects::nonNull)
                .map(String::length)
                .reduce(Integer::sum)
                .orElse(0) < 1024;

        if (isGet) {
            uniqueIds.forEach(x -> {
                pathBuilder.append("&id=");
                pathBuilder.append(
                        UrlUtils.escapeDataString(
                                ObjectUtils.firstNonNull(x, "")));
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

            httpPost.setEntity(new ContentProviderHttpEntity(outputStream -> {
                try (JsonGenerator generator = JsonExtensions.getDefaultMapper().createGenerator(new OutputStreamWriter(outputStream, StandardCharsets.UTF_8))) {
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
            }, ContentType.APPLICATION_JSON, conventions));

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

    public void setTransactionMode(TransactionMode mode) {
        this._txMode = mode;
    }
}
