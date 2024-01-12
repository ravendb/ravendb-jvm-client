package net.ravendb.client.exceptions;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import net.ravendb.client.exceptions.documents.DocumentConflictException;
import net.ravendb.client.exceptions.documents.compilation.IndexCompilationException;
import net.ravendb.client.extensions.JsonExtensions;
import net.ravendb.client.http.RequestExecutor;
import org.apache.commons.io.IOUtils;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.HttpStatus;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

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

    public static void throwException(ClassicHttpResponse response) {
        if (response == null) {
            throw new IllegalArgumentException("Response cannot be null");
        }

        try {
            InputStream stream = RequestExecutor.readAsStream(response);
            String jsonText = IOUtils.toString(stream, StandardCharsets.UTF_8);
            ObjectNode json = (ObjectNode) JsonExtensions.getDefaultMapper().readTree(jsonText);
            ExceptionSchema schema = JsonExtensions.getDefaultMapper().convertValue(json, ExceptionSchema.class);

            if (response.getCode() == HttpStatus.SC_CONFLICT) {
                throwConflict(schema, json);
            }

            Class<?> type = getType(schema.getType());
            if (type == null) {
                throw RavenException.generic(schema.getError(), jsonText);
            }

            RavenException exception;

            try {
                exception = (RavenException) type.getConstructor(String.class).newInstance(schema.getError());
            } catch (Exception e) {
                throw RavenException.generic(schema.getError(), jsonText);
            }

            if (!RavenException.class.isAssignableFrom(type)) {
                throw new RavenException(schema.getError(), exception);
            }

            fillException(exception, json);

            throw exception;

        } catch (IOException e) {
            throw new RavenException(e.getMessage(), e);
        } finally {
            IOUtils.closeQuietly(response, null);
        }
    }

    private static void fillException(Exception exception, ObjectNode json) {
        if (exception instanceof IndexCompilationException) {
            IndexCompilationException indexCompilationException = (IndexCompilationException) exception;
            JsonNode indexDefinitionProperty = json.get("TransformerDefinitionProperty");
            if (indexDefinitionProperty != null) {
                indexCompilationException.setIndexDefinitionProperty(indexDefinitionProperty.asText());
            }

            JsonNode problematicText = json.get("ProblematicText");
            if (problematicText != null) {
                indexCompilationException.setProblematicText(problematicText.asText());
            }
        }

        if (exception instanceof RavenTimeoutException) {
            RavenTimeoutException timeoutException = (RavenTimeoutException) exception;
            JsonNode failImmediately = json.get("FailImmediately");
            if (failImmediately != null) {
                timeoutException.setFailImmediately(failImmediately.asBoolean());
            }
        }
    }


    private static void throwConflict(ExceptionSchema schema, ObjectNode json) {
        if (schema.getType().contains("DocumentConflictException")) {
            throw DocumentConflictException.fromJson(json);
        }

        if (schema.getType().contains("ClusterTransactionConcurrencyException")) {
            ClusterTransactionConcurrencyException ctxConcurrencyException = new ClusterTransactionConcurrencyException(schema.getMessage());

            JsonNode idNode = json.get("Id");
            if (idNode != null && !idNode.isNull()) {
                ctxConcurrencyException.setId(idNode.asText());
            }

            JsonNode expectedChangeVectorNode = json.get("ExpectedChangeVector");
            if (expectedChangeVectorNode != null && !expectedChangeVectorNode.isNull()) {
                ctxConcurrencyException.setExpectedChangeVector(expectedChangeVectorNode.asText());
            }

            JsonNode actualChangeVectorNode = json.get("ActualChangeVector");
            if (actualChangeVectorNode != null && !actualChangeVectorNode.isNull()) {
                ctxConcurrencyException.setActualChangeVector(actualChangeVectorNode.asText());
            }

            JsonNode concurrencyViolationsNode = json.get("ConcurrencyViolations");
            if (concurrencyViolationsNode == null || !concurrencyViolationsNode.isArray()) {
                throw ctxConcurrencyException;
            }

            ArrayNode concurrencyViolationsJsonArray = (ArrayNode) concurrencyViolationsNode;

            ctxConcurrencyException.setConcurrencyViolations(new ClusterTransactionConcurrencyException.ConcurrencyViolation[concurrencyViolationsJsonArray.size()]);

            for (int i = 0; i < concurrencyViolationsJsonArray.size(); i++) {
                JsonNode violation = concurrencyViolationsJsonArray.get(i);
                if (violation == null || violation.isNull()) {
                    continue;
                }

                ClusterTransactionConcurrencyException.ConcurrencyViolation current = new ClusterTransactionConcurrencyException.ConcurrencyViolation();
                ctxConcurrencyException.getConcurrencyViolations()[i] = current;

                JsonNode jsonId = violation.get("Id");
                if (jsonId != null && !jsonId.isNull()) {
                    current.setId(jsonId.asText());
                }

                String typeText = violation.get("Type").asText();
                switch (typeText) {
                    case "Document" :
                        current.setType(ClusterTransactionConcurrencyException.ViolationOnType.DOCUMENT);
                        break;
                    case "CompareExchange":
                        current.setType(ClusterTransactionConcurrencyException.ViolationOnType.COMPARE_EXCHANGE);
                        break;
                    default:
                        throw new IllegalArgumentException("Invalid type: " + typeText);
                }

                JsonNode jsonExpected = violation.get("Expected");
                if (jsonExpected != null && !jsonExpected.isNull()) {
                    current.setExpected(jsonExpected.asLong());
                }

                JsonNode jsonActual = violation.get("Actual");
                if (jsonActual != null && !jsonActual.isNull()) {
                    current.setActual(jsonActual.asLong());
                }
            }

            throw ctxConcurrencyException;
        }

        ConcurrencyException concurrencyException = new ConcurrencyException(schema.getMessage());
        JsonNode idNode = json.get("Id");
        if (idNode != null) {
            concurrencyException.setId(idNode.asText());
        }

        JsonNode expectedChangeVectorNode = json.get("ExpectedChangeVector");
        if (expectedChangeVectorNode != null) {
            concurrencyException.setExpectedChangeVector(expectedChangeVectorNode.asText());
        }

        JsonNode actualChangeVectorNode = json.get("ActualChangeVector");
        if (actualChangeVectorNode != null) {
            concurrencyException.setActualChangeVector(actualChangeVectorNode.asText());
        }

        throw concurrencyException;
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
