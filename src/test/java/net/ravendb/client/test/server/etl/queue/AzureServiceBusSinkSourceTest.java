package net.ravendb.client.test.server.etl.queue;

import net.ravendb.client.documents.operations.etl.queue.AzureServiceBusConnectionSettings;
import net.ravendb.client.documents.operations.etl.queue.AzureServiceBusEntraId;
import net.ravendb.client.documents.operations.etl.queue.AzureServiceBusPasswordless;
import net.ravendb.client.documents.operations.queueSink.AzureServiceBusSinkSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class AzureServiceBusSinkSourceTest {

    @Test
    public void queueReturnsQueueNameWhenNameValid() {
        assertThat(AzureServiceBusSinkSource.queue("my-queue")).isEqualTo("my-queue");
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    public void queueThrowsWhenNameEmpty(String name) {
        assertThatThrownBy(() -> AzureServiceBusSinkSource.queue(name))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void queueThrowsWhenNameContainsSeparator() {
        assertThatThrownBy(() -> AzureServiceBusSinkSource.queue("foo;bar"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void subscriptionEncodesTopicAndSubscription() {
        assertThat(AzureServiceBusSinkSource.subscription("topic", "sub")).isEqualTo("topic;sub");
    }

    private static Stream<Arguments> emptySubscriptionArguments() {
        return Stream.of(
                Arguments.of(null, "sub"),
                Arguments.of("", "sub"),
                Arguments.of("   ", "sub"),
                Arguments.of("topic", null),
                Arguments.of("topic", ""),
                Arguments.of("topic", "   ")
        );
    }

    @ParameterizedTest
    @MethodSource("emptySubscriptionArguments")
    public void subscriptionThrowsWhenArgumentEmpty(String topic, String subscription) {
        assertThatThrownBy(() -> AzureServiceBusSinkSource.subscription(topic, subscription))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest
    @MethodSource("separatorSubscriptionArguments")
    public void subscriptionThrowsWhenArgumentContainsSeparator(String topic, String subscription) {
        assertThatThrownBy(() -> AzureServiceBusSinkSource.subscription(topic, subscription))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static Stream<Arguments> separatorSubscriptionArguments() {
        return Stream.of(
                Arguments.of("to;pic", "sub"),
                Arguments.of("topic", "su;b")
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "Endpoint=sb://ns.servicebus.windows.net/;SharedAccessKeyName=key;SharedAccessKey=abc",
            "endpoint=sb://ns.servicebus.windows.net/;SharedAccessKeyName=key;SharedAccessKey=abc",
            "SharedAccessKeyName=key;Endpoint=sb://ns.servicebus.windows.net/;SharedAccessKey=abc",
            "Endpoint=sb://ns.servicebus.windows.net"
    })
    public void connectionSettingsAcceptValidConnectionString(String connectionString) {
        AzureServiceBusConnectionSettings settings = new AzureServiceBusConnectionSettings();
        settings.setConnectionString(connectionString);

        assertThat(settings.isValidConnection())
                .as("expected valid: %s", connectionString)
                .isTrue();
    }

    /**
     * Client-side validation is intentionally shallow — it just confirms the string contains "sb://".
     * Deeper validation is deferred to the Azure SDK on the server.
     */
    @ParameterizedTest
    @ValueSource(strings = {
            "SharedAccessKeyName=key;SharedAccessKey=abc",                    // no sb://
            "Endpoint=https://ns.servicebus.windows.net/;SharedAccessKey=abc", // wrong scheme
            "nothing-useful-here"
    })
    public void connectionSettingsRejectInvalidConnectionString(String connectionString) {
        AzureServiceBusConnectionSettings settings = new AzureServiceBusConnectionSettings();
        settings.setConnectionString(connectionString);

        assertThat(settings.isValidConnection())
                .as("expected invalid: %s", connectionString)
                .isFalse();
    }

    @Test
    public void connectionSettingsRequireExactlyOneAuthenticationMethod() {
        AzureServiceBusEntraId entraId = new AzureServiceBusEntraId();
        entraId.setNamespace("ns.servicebus.windows.net");
        entraId.setTenantId("tenant");
        entraId.setClientId("client");
        entraId.setClientSecret("secret");

        AzureServiceBusConnectionSettings entraOnly = new AzureServiceBusConnectionSettings();
        entraOnly.setEntraId(entraId);
        assertThat(entraOnly.isValidConnection()).isTrue();
        assertThat(entraOnly.getServiceBusUrl()).isEqualTo("sb://ns.servicebus.windows.net/");

        AzureServiceBusPasswordless passwordless = new AzureServiceBusPasswordless();
        passwordless.setNamespace("ns.servicebus.windows.net");

        AzureServiceBusConnectionSettings passwordlessOnly = new AzureServiceBusConnectionSettings();
        passwordlessOnly.setPasswordless(passwordless);
        assertThat(passwordlessOnly.isValidConnection()).isTrue();

        // Two methods configured at once is invalid.
        AzureServiceBusConnectionSettings both = new AzureServiceBusConnectionSettings();
        both.setEntraId(entraId);
        both.setPasswordless(passwordless);
        assertThat(both.isValidConnection()).isFalse();

        // Nothing configured is invalid too.
        assertThat(new AzureServiceBusConnectionSettings().isValidConnection()).isFalse();
    }

    @Test
    public void incompleteEntraIdIsRejected() {
        AzureServiceBusEntraId entraId = new AzureServiceBusEntraId();
        entraId.setNamespace("ns.servicebus.windows.net");
        entraId.setTenantId("tenant");
        // clientId / clientSecret missing

        AzureServiceBusConnectionSettings settings = new AzureServiceBusConnectionSettings();
        settings.setEntraId(entraId);

        assertThat(settings.isValidConnection()).isFalse();
    }

    @Test
    public void serviceBusUrlIsExtractedFromConnectionString() {
        AzureServiceBusConnectionSettings settings = new AzureServiceBusConnectionSettings();
        settings.setConnectionString("Endpoint=sb://ns.servicebus.windows.net/;SharedAccessKeyName=key;SharedAccessKey=abc");

        assertThat(settings.getServiceBusUrl()).isEqualTo("sb://ns.servicebus.windows.net/");

        // No trailing separator - the rest of the string is the endpoint.
        AzureServiceBusConnectionSettings noSeparator = new AzureServiceBusConnectionSettings();
        noSeparator.setConnectionString("Endpoint=sb://ns.servicebus.windows.net");
        assertThat(noSeparator.getServiceBusUrl()).isEqualTo("sb://ns.servicebus.windows.net");
    }
}
