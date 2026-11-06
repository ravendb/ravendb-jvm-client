package net.ravendb.client.exceptions;

import com.fasterxml.jackson.databind.node.ObjectNode;
import net.ravendb.client.exceptions.commercial.LicenseLimitException;
import net.ravendb.client.exceptions.commercial.LimitType;
import net.ravendb.client.exceptions.database.BackupAlreadyRunningException;
import net.ravendb.client.exceptions.documents.compilation.IndexCompilationException;
import net.ravendb.client.extensions.JsonExtensions;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.http.message.BasicClassicHttpResponse;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;

public class ExceptionDispatcherTest {

    @Test
    public void queryToolFailedExceptionIsDispatchedAsAiException() {
        ExceptionDispatcher.ExceptionSchema schema = new ExceptionDispatcher.ExceptionSchema();
        schema.setUrl("http://127.0.0.1:8080/databases/db1/ai/agent");
        schema.setType("Raven.Client.Exceptions.QueryToolFailedException");
        schema.setMessage("The field 'Name1' is not indexed in 'Customers/ByName'");
        schema.setError("Raven.Client.Exceptions.QueryToolFailedException: The field 'Name1' is not indexed in 'Customers/ByName'");

        RavenException exception = ExceptionDispatcher.get(schema, 500);

        assertThat(exception)
                .isExactlyInstanceOf(QueryToolFailedException.class)
                .isInstanceOf(AiException.class);
        assertThat(exception.getMessage()).contains("The field 'Name1' is not indexed in 'Customers/ByName'");
    }

    @Test
    public void rateLimitExceptionGetsRetryAfterAndStatusCode() {
        ObjectNode json = serverError("Raven.Client.Exceptions.RateLimitException", "rate limited");
        json.put("RetryAfter", "00:01:30");
        json.put("StatusCode", "TooManyRequests");

        RavenException exception = ExceptionDispatcher.get(json, 500, null);

        assertThat(exception)
                .isExactlyInstanceOf(RateLimitException.class)
                .isInstanceOf(TooManyRequestsException.class);
        RateLimitException rateLimitException = (RateLimitException) exception;
        assertThat(rateLimitException.getRetryAfter())
                .isEqualTo(Duration.ofSeconds(90));
        assertThat(rateLimitException.getStatusCode())
                .isEqualTo(429);
    }

    @Test
    public void unsuccessfulAiRequestStatusCodeIsParsedFromEnumName() {
        ObjectNode json = serverError("Raven.Client.Exceptions.InsufficientQuotaException", "quota");
        json.put("StatusCode", "TooManyRequests");

        RavenException exception = ExceptionDispatcher.get(json, 500, null);
        assertThat(exception)
                .isExactlyInstanceOf(InsufficientQuotaException.class);
        assertThat(((UnsuccessfulAiRequestException) exception).getStatusCode())
                .isEqualTo(429);

        json = serverError("Raven.Client.Exceptions.TooManyRequestsException", "bad gateway");
        json.put("StatusCode", "BadGateway");
        exception = ExceptionDispatcher.get(json, 500, null);
        assertThat(((UnsuccessfulAiRequestException) exception).getStatusCode())
                .isEqualTo(502);

        // unknown value must not overwrite the default status code
        json = serverError("Raven.Client.Exceptions.TooManyTokensException", "tokens");
        json.put("StatusCode", "SomethingElse");
        exception = ExceptionDispatcher.get(json, 500, null);
        assertThat(((UnsuccessfulAiRequestException) exception).getStatusCode())
                .isEqualTo(429);
    }

    @Test
    public void backupAlreadyRunningExceptionIsFilled() {
        ObjectNode json = serverError("Raven.Client.Exceptions.Database.BackupAlreadyRunningException", "backup is running");
        json.put("OperationId", 17L);
        json.put("NodeTag", "B");

        RavenException exception = ExceptionDispatcher.get(json, 500, null);

        assertThat(exception)
                .isExactlyInstanceOf(BackupAlreadyRunningException.class);
        BackupAlreadyRunningException backupException = (BackupAlreadyRunningException) exception;
        assertThat(backupException.getOperationId())
                .isEqualTo(17L);
        assertThat(backupException.getNodeTag())
                .isEqualTo("B");
    }

    @Test
    public void licenseLimitExceptionGetsLimitType() {
        ObjectNode json = serverError("Raven.Client.Exceptions.Commercial.LicenseLimitException", "limit");
        json.put("LimitType", "PowerBI");

        RavenException exception = ExceptionDispatcher.get(json, 402, null);

        assertThat(exception)
                .isExactlyInstanceOf(LicenseLimitException.class);
        assertThat(((LicenseLimitException) exception).getLimitType())
                .isEqualTo(LimitType.POWER_BI);

        json.put("LimitType", "AdditionalAssembliesFromNuGet");
        exception = ExceptionDispatcher.get(json, 402, null);
        assertThat(((LicenseLimitException) exception).getLimitType())
                .isEqualTo(LimitType.ADDITIONAL_ASSEMBLIES_FROM_NUGET);

        // operation results pass the limit type separately (OperationExceptionResult.LimitType)
        ExceptionDispatcher.ExceptionSchema schema = new ExceptionDispatcher.ExceptionSchema();
        schema.setUrl("http://localhost:8080");
        schema.setType("Raven.Client.Exceptions.Commercial.LicenseLimitException");
        schema.setMessage("limit");
        schema.setError("limit");
        exception = ExceptionDispatcher.get(schema, 402, null, LimitType.SNAPSHOT_BACKUP);
        assertThat(((LicenseLimitException) exception).getLimitType())
                .isEqualTo(LimitType.SNAPSHOT_BACKUP);
    }

    @Test
    public void limitTypeUsesCSharpNames() {
        assertThat(JsonExtensions.getDefaultMapper().valueToTree(LimitType.SSO).asText())
                .isEqualTo("Sso");
        assertThat(JsonExtensions.getDefaultMapper().valueToTree(LimitType.POWER_BI).asText())
                .isEqualTo("PowerBI");
        assertThat(LimitType.values())
                .hasSize(58);
    }

    @Test
    public void indexCompilationExceptionReadsIndexDefinitionProperty() {
        ObjectNode json = serverError("Raven.Client.Exceptions.Documents.Compilation.IndexCompilationException", "compilation");
        json.put("IndexDefinitionProperty", "Maps");
        json.put("ProblematicText", "from doc in docs");

        RavenException exception = ExceptionDispatcher.get(json, 500, null);

        assertThat(exception)
                .isExactlyInstanceOf(IndexCompilationException.class);
        assertThat(((IndexCompilationException) exception).getIndexDefinitionProperty())
                .isEqualTo("Maps");
        assertThat(((IndexCompilationException) exception).getProblematicText())
                .isEqualTo("from doc in docs");
    }

    @Test
    public void throwExceptionFillsExceptionFromResponse() {
        ObjectNode json = serverError("Raven.Client.Exceptions.RateLimitException", "rate limited");
        json.put("RetryAfter", "00:00:05");

        Throwable thrown = catchThrowable(() -> ExceptionDispatcher.throwException(response(429, json.toString())));

        assertThat(thrown)
                .isExactlyInstanceOf(RateLimitException.class);
        assertThat(((RateLimitException) thrown).getRetryAfter())
                .isEqualTo(Duration.ofSeconds(5));
    }

    @Test
    public void throwExceptionValidatesResponse() {
        assertThatThrownBy(() -> ExceptionDispatcher.throwException(response(500, "<html>not json</html>")))
                .isExactlyInstanceOf(IllegalStateException.class)
                .hasMessageStartingWith("Cannot parse the '500' response. Content: <html>not json</html>");

        assertThatThrownBy(() -> ExceptionDispatcher.throwException(response(500, "{\"Message\":\"m\",\"Error\":\"e\"}")))
                .isExactlyInstanceOf(BadResponseException.class)
                .hasMessageContaining("response property 'Type' is null");
    }

    private static ObjectNode serverError(String type, String message) {
        ObjectNode json = JsonExtensions.getDefaultMapper().createObjectNode();
        json.put("Url", "/databases/db1/test");
        json.put("Type", type);
        json.put("Message", message);
        json.put("Error", type + ": " + message);
        return json;
    }

    private static ClassicHttpResponse response(int code, String content) {
        BasicClassicHttpResponse response = new BasicClassicHttpResponse(code);
        response.setEntity(new StringEntity(content, ContentType.APPLICATION_JSON));
        return response;
    }
}
