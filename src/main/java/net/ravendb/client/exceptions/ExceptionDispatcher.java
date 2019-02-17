package net.ravendb.client.exceptions;

import com.fasterxml.jackson.databind.JsonNode;
import net.ravendb.client.exceptions.documents.DocumentConflictException;
import net.ravendb.client.exceptions.documents.compilation.IndexCompilationException;
import net.ravendb.client.extensions.JsonExtensions;
import net.ravendb.client.http.RequestExecutor;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;

import java.io.IOException;
import java.io.InputStream;

public class ExceptionDispatcher {

    public static RavenException get(ExceptionSchema schema, int code) {
        return get(schema, code, null);
    }

    public static RavenException get(ExceptionSchema schema, int code, Exception inner) {
        String message = schema.getMessage();
        String typeAsString = schema.getType();

        if (code == HttpStatus.SC_CONFLICT) {
            if (typeAsString.contains("DocumentConflictException")) {
                return DocumentConflictException.fromMessage(message);
            }
            return new ConcurrencyException(message);
        }

        String error = schema.getError() + System.lineSeparator() + "The server at " + schema.getUrl() + " responded with status code: " + code;

        Class<?> type = getType(typeAsString);
        if (type == null) {
            return new RavenException(error, inner);
        }

        RavenException exception;
        try {
            exception = (RavenException) type.getConstructor(String.class).newInstance(error);
        } catch (Exception e) {
            return new RavenException(error, inner);
        }

        if (!RavenException.class.isAssignableFrom(type)) {
            return new RavenException(error, exception);
        }

        return exception;
    }

    public static void throwException(CloseableHttpResponse response) {
        if (response == null) {
            throw new IllegalArgumentException("Response cannot be null");
        }

        try {
            InputStream stream = RequestExecutor.readAsStream(response);
            String json = IOUtils.toString(stream, "UTF-8");
            ExceptionSchema schema = JsonExtensions.getDefaultMapper().readValue(json, ExceptionSchema.class);

            if (response.getStatusLine().getStatusCode() == HttpStatus.SC_CONFLICT) {
                throwConflict(schema, json);
            }

            Class<?> type = getType(schema.getType());
            if (type == null) {
                throw RavenException.generic(schema.getError(), json);
            }

            RavenException exception;

            try {
                exception = (RavenException) type.getConstructor(String.class).newInstance(schema.getError());
            } catch (Exception e) {
                throw RavenException.generic(schema.getError(), json);
            }

            if (!RavenException.class.isAssignableFrom(type)) {
                throw new RavenException(schema.getError(), exception);
            }

            if (IndexCompilationException.class.equals(type)) {
                IndexCompilationException indexCompilationException = (IndexCompilationException) exception;
                JsonNode jsonNode = JsonExtensions.getDefaultMapper().readTree(json);
                JsonNode indexDefinitionProperty = jsonNode.get("TransformerDefinitionProperty");
                if (indexDefinitionProperty != null) {
                    indexCompilationException.setIndexDefinitionProperty(indexDefinitionProperty.asText());
                }

                JsonNode problematicText = jsonNode.get("ProblematicText");
                if (problematicText != null) {
                    indexCompilationException.setProblematicText(problematicText.asText());
                }

                throw indexCompilationException;
            }

            throw exception;

        } catch (IOException e) {
            throw new RavenException(e.getMessage(), e);
        } finally {
            IOUtils.closeQuietly(response);
        }
    }


    private static void throwConflict(ExceptionSchema schema, String json) {
        if (schema.getType().contains("DocumentConflictException")) {
            throw DocumentConflictException.fromJson(json);
        }
        throw new ConcurrencyException(schema.getError());
    }


    private static Class<?> getType(String typeAsString) {
        if ("System.TimeoutException".equals(typeAsString)) {
            return TimeoutException.class;
        }
        String prefix = "Raven.Client.Exceptions.";
        if (typeAsString.startsWith(prefix)) {
            String exceptionName = typeAsString.substring(prefix.length());
            if (exceptionName.contains(".")) {
                String[] tokens = exceptionName.split("\\.");
                for (int i = 0; i < tokens.length - 1; i++) {
                    tokens[i] = tokens[i].toLowerCase();
                }
                exceptionName = String.join(".", tokens);
            }

            try {
                return Class.forName(RavenException.class.getPackage().getName() + "." + exceptionName);
            } catch (Exception e) {
                return null;
            }
        } else {
            return null;
        }
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
