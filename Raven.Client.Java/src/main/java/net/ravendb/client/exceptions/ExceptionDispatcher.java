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

    public static Exception get(ExceptionSchema schema, int httpStatusCode) {
        //TODO: get
        return null;
    }


    public static class ExceptionSchema {
        private String url;
        private String type;
        private String message;
        private String error;

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public String getError() {
            return error;
        }

        public void setError(String error) {
            this.error = error;
        }
    }
}
