package net.ravendb.client.test.server.etl.queue;

import net.ravendb.client.documents.operations.etl.queue.AzureServiceBusConnectionSettings;
import net.ravendb.client.documents.operations.etl.queue.AzureServiceBusEntraId;
import net.ravendb.client.documents.operations.etl.queue.AzureServiceBusPasswordless;
import net.ravendb.client.documents.operations.etl.queue.QueueBrokerType;
import net.ravendb.client.documents.operations.etl.queue.QueueConnectionString;
import net.ravendb.client.documents.operations.queueSink.AzureServiceBusSinkSource;
import net.ravendb.client.extensions.JsonExtensions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
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

    @Test
    public void brokerTypeIsSentUsingTheServerName() throws Exception {
        String json = JsonExtensions.getDefaultMapper().writeValueAsString(entraIdConnectionString());

        assertThat(json).contains("\"BrokerType\":\"AzureServiceBus\"");

        QueueConnectionString parsed =
                JsonExtensions.getDefaultMapper().readValue(json, QueueConnectionString.class);
        assertThat(parsed.getBrokerType()).isEqualTo(QueueBrokerType.AZURE_SERVICE_BUS);
    }

    @Test
    public void connectionStringRoundTripsAzureServiceBusSettings() throws Exception {
        String json = JsonExtensions.getDefaultMapper().writeValueAsString(entraIdConnectionString());

        QueueConnectionString parsed =
                JsonExtensions.getDefaultMapper().readValue(json, QueueConnectionString.class);

        AzureServiceBusConnectionSettings settings = parsed.getAzureServiceBusConnectionSettings();
        assertThat(settings).isNotNull();
        assertThat(settings.getEntraId()).isNotNull();
        assertThat(settings.getEntraId().getNamespace()).isEqualTo("ns.servicebus.windows.net");
        assertThat(settings.getEntraId().getTenantId()).isEqualTo("tenant");
        assertThat(settings.getEntraId().getClientId()).isEqualTo("client");
        assertThat(settings.getEntraId().getClientSecret()).isEqualTo("secret");
        assertThat(settings.isValidConnection()).isTrue();
    }

    /**
     * {@code isValidConnection()} and {@code getServiceBusUrl()} are plain methods in C#, so there is no
     * {@code [JsonIgnore]} upstream to mirror. The Java bean naming makes Jackson pick them up as
     * properties, and the {@code @JsonIgnore} that suppresses them exists only on this side - pin it.
     */
    @Test
    public void derivedHelpersAreNotSerialized() throws Exception {
        String json = JsonExtensions.getDefaultMapper().writeValueAsString(entraIdConnectionString());

        assertThat(json)
                .doesNotContain("ServiceBusUrl")
                .doesNotContain("ValidConnection");
    }

    /**
     * Without {@code @JsonIgnore} on {@code getServiceBusUrl()}, serializing settings that carry no
     * namespace fails inside Jackson with {@code IllegalStateException("No namespace provided")}.
     */
    @Test
    public void settingsWithoutNamespaceSerializeWithoutThrowing() {
        QueueConnectionString connectionString = new QueueConnectionString();
        connectionString.setName("asb-cs");
        connectionString.setBrokerType(QueueBrokerType.AZURE_SERVICE_BUS);
        connectionString.setAzureServiceBusConnectionSettings(new AzureServiceBusConnectionSettings());

        assertThatCode(() -> JsonExtensions.getDefaultMapper().writeValueAsString(connectionString))
                .doesNotThrowAnyException();
    }

    /**
     * The other branch of the same trap: a connection string with no {@code sb://} endpoint makes
     * {@code getServiceBusUrl()} throw {@code IllegalStateException("No endpoint provided")}.
     */
    @Test
    public void settingsWithoutEndpointSerializeWithoutThrowing() {
        AzureServiceBusConnectionSettings settings = new AzureServiceBusConnectionSettings();
        settings.setConnectionString("SharedAccessKeyName=key;SharedAccessKey=abc");

        assertThatCode(() -> JsonExtensions.getDefaultMapper().writeValueAsString(settings))
                .doesNotThrowAnyException();
    }

    private static QueueConnectionString entraIdConnectionString() {
        AzureServiceBusEntraId entraId = new AzureServiceBusEntraId();
        entraId.setNamespace("ns.servicebus.windows.net");
        entraId.setTenantId("tenant");
        entraId.setClientId("client");
        entraId.setClientSecret("secret");

        AzureServiceBusConnectionSettings settings = new AzureServiceBusConnectionSettings();
        settings.setEntraId(entraId);

        QueueConnectionString connectionString = new QueueConnectionString();
        connectionString.setName("asb-cs");
        connectionString.setBrokerType(QueueBrokerType.AZURE_SERVICE_BUS);
        connectionString.setAzureServiceBusConnectionSettings(settings);
        return connectionString;
    }
}
