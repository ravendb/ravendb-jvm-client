package net.ravendb.client.connection;

import com.google.common.base.Throwables;
import net.ravendb.abstractions.basic.Reference;
import net.ravendb.abstractions.connection.ErrorResponseException;
import org.apache.commons.lang.ArrayUtils;

import java.net.SocketException;
import java.net.SocketTimeoutException;

public class HttpConnectionHelper {
    public static boolean isHttpStatus(Exception e, int... httpStatusCode) {
        if (e instanceof ErrorResponseException) {
            ErrorResponseException hoe = (ErrorResponseException) e;
            if (ArrayUtils.contains(httpStatusCode, hoe.getStatusCode())) {
                return true;
            }
        }
        return false;
    }

    public static boolean isServerDown(Exception e, Reference<Boolean> timeout) {
        timeout.value = Boolean.FALSE;
        Throwable rootCause = Throwables.getRootCause(e);
        if (rootCause instanceof SocketTimeoutException) {
            timeout.value = Boolean.TRUE;
            return true;
        }
        if (rootCause instanceof SocketException) {
            return true;
        }
        return false;
    }

}
