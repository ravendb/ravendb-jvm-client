package net.ravendb.client.extensions;

import net.ravendb.client.Constants;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.Header;
import org.apache.http.client.methods.CloseableHttpResponse;

import java.util.Map;

public class HttpExtensions {

    public static String getRequiredEtagHeader(CloseableHttpResponse response) {
        if (response.containsHeader(Constants.Headers.ETAG)) {
            Header firstHeader = response.getFirstHeader(Constants.Headers.ETAG);
            String value = firstHeader.getValue();
            if (StringUtils.isNotEmpty(value)) {
                return etagHeaderToChangeVector(value);
            } else {
                throw new IllegalStateException("Response did't had an ETag header");
            }
        } else {
            throw new IllegalStateException("Response did't had an ETag header");
        }
    }

    public static String getEtagHeader(CloseableHttpResponse response) {
        if (response.containsHeader(Constants.Headers.ETAG)) {
            Header[] headers = response.getHeaders(Constants.Headers.ETAG);
            if (headers.length > 0) {
                return etagHeaderToChangeVector(headers[0].getValue());
            }
        }

        return null;
    }

    private static String etagHeaderToChangeVector(String responseHeader) {
        if (StringUtils.isEmpty(responseHeader)) {
            throw new IllegalStateException("Response did't had an ETag header");
        }

        if (responseHeader.startsWith("\"")) {
            return responseHeader.substring(1, responseHeader.length() - 1);
        }

        return responseHeader;
    }

    public static String getEtagHeader(Map<String, String> headers) {
        if (headers.containsKey(Constants.Headers.ETAG)) {
            return etagHeaderToChangeVector(headers.get(Constants.Headers.ETAG));
        } else {
            return null;
        }
    }

    public static Boolean getBooleanHeader(CloseableHttpResponse response, String header) {
        if (response.containsHeader(header)) {
            Header firstHeader = response.getFirstHeader(header);
            String value = firstHeader.getValue();
            return Boolean.valueOf(value);
        } else {
            return null;
        }
    }

}
