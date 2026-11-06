package net.ravendb.client.exceptions;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import net.ravendb.client.exceptions.commercial.LicenseLimitException;
import net.ravendb.client.exceptions.commercial.LimitType;
import net.ravendb.client.exceptions.database.BackupAlreadyRunningException;
import net.ravendb.client.exceptions.documents.DocumentConflictException;
import net.ravendb.client.exceptions.documents.compilation.IndexCompilationException;
import net.ravendb.client.extensions.JsonExtensions;
import net.ravendb.client.http.RequestExecutor;
import net.ravendb.client.util.TimeUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.HttpStatus;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class ExceptionDispatcher {

    public static RavenException get(ExceptionSchema schema, int code) {
        return get(schema, code, null);
    }

    public static RavenException get(ExceptionSchema schema, int code, Exception inner) {
        return get(schema, code, inner, null);
    }

    public static RavenException get(ExceptionSchema schema, int code, Exception inner, LimitType limitType) {
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

        Exception exception;
        try {
            exception = (Exception) type.getConstructor(String.class).newInstance(error);
        } catch (Exception e) {
            return new RavenException(error);
        }

        if (!(exception instanceof RavenException)) {
            return new RavenException(error, exception);
        }

        if (exception instanceof LicenseLimitException && limitType != null) {
            ((LicenseLimitException) exception).setLimitType(limitType);
        }

        return (RavenException) exception;
    }

    public static RavenException get(ObjectNode json, int code, Exception inner) {
        ExceptionSchema schema = getExceptionSchema(code, json);

        RavenException exception = get(schema, code, inner);

        fillException(exception, json);

        return exception;
    }

    public static void throwException(ClassicHttpResponse response) {
        if (response == null) {
            throw new IllegalArgumentException("Response cannot be null");
        }

        try {
            InputStream stream = RequestExecutor.readAsStream(response);
            String jsonText = IOUtils.toString(stream, StandardCharsets.UTF_8);
            ObjectNode json = getJson(response.getCode(), jsonText);
            ExceptionSchema schema = getExceptionSchema(response.getCode(), json);

            if (response.getCode() == HttpStatus.SC_CONFLICT) {
                throwConflict(schema, json);
            }

            Class<?> type = getType(schema.getType());
            if (type == null) {
                throw RavenException.generic(schema.getError(), jsonText);
            }

            Exception exception;

            try {
                exception = (Exception) type.getConstructor(String.class).newInstance(schema.getError());
            } catch (Exception e) {
                throw RavenException.generic(schema.getError(), jsonText);
            }

            if (!(exception instanceof RavenException)) {
                throw new RavenException(schema.getError(), exception);
            }

            fillException(exception, json);

            throw (RavenException) exception;

        } catch (IOException e) {
            throw new RavenException(e.getMessage(), e);
        } finally {
            IOUtils.closeQuietly(response, null);
        }
    }

    private static ObjectNode getJson(int code, String content) {
        JsonNode json;
        try {
            json = JsonExtensions.getDefaultMapper().readTree(content);
        } catch (Exception e) {
            throw new IllegalStateException("Cannot parse the '" + code + "' response. Content: " + content, e);
        }

        if (!(json instanceof ObjectNode)) {
            throw new IllegalStateException("Cannot parse the '" + code + "' response. Content: " + content);
        }

        return (ObjectNode) json;
    }

    private static ExceptionSchema getExceptionSchema(int code, ObjectNode json) {
        ExceptionSchema schema;
        try {
            schema = JsonExtensions.getDefaultMapper().convertValue(json, ExceptionSchema.class);
        } catch (Exception e) {
            throw new IllegalStateException("Cannot deserialize the " + code + " response. JSON: " + json, e);
        }

        if (schema == null) {
            throw new BadResponseException("After deserialization the " + code + " response is null. JSON: " + json);
        }

        if (schema.getMessage() == null) {
            throw new BadResponseException("After deserialization the " + code + " response property 'Message' is null. JSON: " + json);
        }

        if (schema.getError() == null) {
            throw new BadResponseException("After deserialization the " + code + " response property 'Error' is null. JSON: " + json);
        }

        if (schema.getType() == null) {
            throw new BadResponseException("After deserialization the " + code + " response property 'Type' is null. JSON: " + json);
        }

        return schema;
    }

    private static void fillException(Exception exception, ObjectNode json) {
        if (exception instanceof RateLimitException) {
            RateLimitException rateLimitException = (RateLimitException) exception;
            rateLimitException.setStatusCode(429);
            String retryAfter = getString(json, "RetryAfter");
            if (retryAfter != null) {
                try {
                    rateLimitException.setRetryAfter(TimeUtils.timeSpanToDuration(retryAfter));
                } catch (Exception ignored) {
                    // unparsable value - left unset, like TimeSpan.TryParse in the C# client
                }
            }
        } else if (exception instanceof UnsuccessfulAiRequestException) {
            UnsuccessfulAiRequestException unsuccessfulAiRequestException = (UnsuccessfulAiRequestException) exception;
            Integer statusCode = parseHttpStatusCode(json.get("StatusCode"));
            if (statusCode != null) {
                unsuccessfulAiRequestException.setStatusCode(statusCode);
            }
        } else if (exception instanceof RefusedToAnswerException) {
            RefusedToAnswerException refusedToAnswerException = (RefusedToAnswerException) exception;
            refusedToAnswerException.refusal = getString(json, "Refusal");
            refusedToAnswerException.finishReason = getString(json, "FinishReason");
        } else if (exception instanceof IndexCompilationException) {
            IndexCompilationException indexCompilationException = (IndexCompilationException) exception;
            indexCompilationException.setIndexDefinitionProperty(getString(json, "IndexDefinitionProperty"));
            indexCompilationException.setProblematicText(getString(json, "ProblematicText"));
        } else if (exception instanceof RavenTimeoutException) {
            RavenTimeoutException timeoutException = (RavenTimeoutException) exception;
            JsonNode failImmediately = json.get("FailImmediately");
            if (failImmediately != null && failImmediately.isBoolean()) {
                timeoutException.setFailImmediately(failImmediately.asBoolean());
            }
        } else if (exception instanceof BackupAlreadyRunningException) {
            BackupAlreadyRunningException backupAlreadyRunningException = (BackupAlreadyRunningException) exception;
            JsonNode operationId = json.get("OperationId");
            if (operationId != null && operationId.isNumber()) {
                backupAlreadyRunningException.setOperationId(operationId.asLong());
            }
            backupAlreadyRunningException.setNodeTag(getString(json, "NodeTag"));
        } else if (exception instanceof LicenseLimitException) {
            LicenseLimitException licenseLimitException = (LicenseLimitException) exception;
            LimitType limitType = parseLimitType(getString(json, "LimitType"));
            if (limitType != null) {
                licenseLimitException.setLimitType(limitType);
            }
        }
    }

    private static String getString(ObjectNode json, String propertyName) {
        JsonNode node = json.get(propertyName);
        if (node == null || node.isNull()) {
            return null;
        }

        return node.asText();
    }

    private static LimitType parseLimitType(String limitType) {
        if (limitType == null) {
            return null;
        }

        if (StringUtils.isNumeric(limitType)) {
            try {
                int ordinal = Integer.parseInt(limitType);
                LimitType[] values = LimitType.values();
                return ordinal < values.length ? values[ordinal] : null;
            } catch (NumberFormatException e) {
                return null;
            }
        }

        try {
            return JsonExtensions.getDefaultMapper().convertValue(limitType, LimitType.class);
        } catch (Exception e) {
            return null;
        }
    }

    private static Integer parseHttpStatusCode(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }

        if (node.isIntegralNumber()) {
            return node.asInt();
        }

        String text = node.asText();
        if (StringUtils.isNumeric(text)) {
            try {
                return Integer.parseInt(text);
            } catch (NumberFormatException e) {
                return null;
            }
        }

        // the server writes System.Net.HttpStatusCode by name (e.g. "TooManyRequests")
        return HTTP_STATUS_CODES.get(text);
    }

    private static final Map<String, Integer> HTTP_STATUS_CODES = new HashMap<>();

    static {
        Object[] codes = new Object[] {
                "Continue", 100, "SwitchingProtocols", 101, "Processing", 102, "EarlyHints", 103,
                "OK", 200, "Created", 201, "Accepted", 202, "NonAuthoritativeInformation", 203, "NoContent", 204,
                "ResetContent", 205, "PartialContent", 206, "MultiStatus", 207, "AlreadyReported", 208, "IMUsed", 226,
                "MultipleChoices", 300, "Ambiguous", 300, "MovedPermanently", 301, "Moved", 301, "Found", 302, "Redirect", 302,
                "SeeOther", 303, "RedirectMethod", 303, "NotModified", 304, "UseProxy", 305, "Unused", 306,
                "TemporaryRedirect", 307, "RedirectKeepVerb", 307, "PermanentRedirect", 308,
                "BadRequest", 400, "Unauthorized", 401, "PaymentRequired", 402, "Forbidden", 403, "NotFound", 404,
                "MethodNotAllowed", 405, "NotAcceptable", 406, "ProxyAuthenticationRequired", 407, "RequestTimeout", 408,
                "Conflict", 409, "Gone", 410, "LengthRequired", 411, "PreconditionFailed", 412, "RequestEntityTooLarge", 413,
                "RequestUriTooLong", 414, "UnsupportedMediaType", 415, "RequestedRangeNotSatisfiable", 416,
                "ExpectationFailed", 417, "MisdirectedRequest", 421, "UnprocessableEntity", 422, "UnprocessableContent", 422,
                "Locked", 423, "FailedDependency", 424, "UpgradeRequired", 426, "PreconditionRequired", 428,
                "TooManyRequests", 429, "RequestHeaderFieldsTooLarge", 431, "UnavailableForLegalReasons", 451,
                "InternalServerError", 500, "NotImplemented", 501, "BadGateway", 502, "ServiceUnavailable", 503,
                "GatewayTimeout", 504, "HttpVersionNotSupported", 505, "VariantAlsoNegotiates", 506,
                "InsufficientStorage", 507, "LoopDetected", 508, "NotExtended", 510, "NetworkAuthenticationRequired", 511
        };

        for (int i = 0; i < codes.length; i += 2) {
            HTTP_STATUS_CODES.put((String) codes[i], (Integer) codes[i + 1]);
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

        ConcurrencyException concurrencyException = new ConcurrencyException(schema.getMessage(), new RavenException(schema.getError()));
        JsonNode idNode = json.get("Id");
        if (idNode != null) {
            concurrencyException.setId(idNode.asText());
        }

        JsonNode expectedChangeVectorNode = json.get("ExpectedChangeVector");
        if (expectedChangeVectorNode != null && !expectedChangeVectorNode.isNull()) {
            concurrencyException.setExpectedChangeVector(expectedChangeVectorNode.asText());
        }

        JsonNode actualChangeVectorNode = json.get("ActualChangeVector");
        if (actualChangeVectorNode != null && !actualChangeVectorNode.isNull()) {
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
