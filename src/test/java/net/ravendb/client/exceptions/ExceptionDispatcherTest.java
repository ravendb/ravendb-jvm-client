package net.ravendb.client.exceptions;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

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
}
