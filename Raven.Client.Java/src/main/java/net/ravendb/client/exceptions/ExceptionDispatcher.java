package net.ravendb.client.exceptions;

import net.ravendb.client.primitives.ExceptionsUtils;
import org.apache.commons.io.IOUtils;
import org.apache.http.client.methods.CloseableHttpResponse;

import java.io.IOException;

//TODO:
public class ExceptionDispatcher {

    public static void throwException(CloseableHttpResponse response) {
        try {
            String string = IOUtils.toString(response.getEntity().getContent(), "utf-8");
            throw new RuntimeException(string);
        } catch (IOException e) {
            throw ExceptionsUtils.unwrapException(e);
        }

    }
}
