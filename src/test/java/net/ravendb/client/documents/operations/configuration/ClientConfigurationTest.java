package net.ravendb.client.documents.operations.configuration;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.DocumentStore;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.http.LoadBalanceBehavior;
import net.ravendb.client.http.NodeSelector;
import net.ravendb.client.http.ReadBalanceBehavior;
import net.ravendb.client.http.RequestExecutor;
import net.ravendb.client.infrastructure.DisabledOnPullRequest;
import net.ravendb.client.infrastructure.entities.User;
import net.ravendb.client.serverwide.operations.configuration.GetServerWideClientConfigurationOperation;
import net.ravendb.client.serverwide.operations.configuration.PutServerWideClientConfigurationOperation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.lang.reflect.Field;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

@DisabledOnPullRequest
public class ClientConfigurationTest extends RemoteTestBase {

    @Test
    public void canSaveAndReadServerWideClientConfiguration() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {

            ClientConfiguration configurationToSave = new ClientConfiguration();
            configurationToSave.setMaxNumberOfRequestsPerSession(80);
            configurationToSave.setReadBalanceBehavior(ReadBalanceBehavior.FASTEST_NODE);
            configurationToSave.setDisabled(true);
            configurationToSave.setLoadBalanceBehavior(LoadBalanceBehavior.NONE);
            configurationToSave.setLoadBalancerContextSeed(0);

            PutServerWideClientConfigurationOperation saveOperation = new PutServerWideClientConfigurationOperation(configurationToSave);

            store.maintenance().server().send(saveOperation);

            GetServerWideClientConfigurationOperation operation = new GetServerWideClientConfigurationOperation();
            ClientConfiguration newConfiguration = store.maintenance().server().send(operation);

            assertThat(newConfiguration)
                    .isNotNull();

            assertThat(newConfiguration.isDisabled())
                    .isTrue();

            assertThat(newConfiguration.getMaxNumberOfRequestsPerSession())
                    .isEqualTo(80);

            assertThat(newConfiguration.getLoadBalancerContextSeed())
                    .isEqualTo(0);

            assertThat(newConfiguration.getLoadBalanceBehavior())
                    .isEqualTo(LoadBalanceBehavior.NONE);

            assertThat(newConfiguration.getReadBalanceBehavior())
                    .isEqualTo(ReadBalanceBehavior.FASTEST_NODE);
        }
    }

    @Test
    public void canHandleNoConfiguration() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            GetClientConfigurationOperation operation = new GetClientConfigurationOperation();
            GetClientConfigurationOperation.Result result = store.maintenance().send(operation);

            assertThat(result.getEtag())
                    .isNotNull();
        }
    }

    @Test
    public void canSaveAndReadClientConfiguration() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {

            ClientConfiguration configurationToSave = new ClientConfiguration();
            configurationToSave.setEtag(123L);
            configurationToSave.setMaxNumberOfRequestsPerSession(80);
            configurationToSave.setReadBalanceBehavior(ReadBalanceBehavior.FASTEST_NODE);
            configurationToSave.setDisabled(true);

            PutClientConfigurationOperation saveOperation = new PutClientConfigurationOperation(configurationToSave);

            store.maintenance().send(saveOperation);

            GetClientConfigurationOperation operation = new GetClientConfigurationOperation();
            GetClientConfigurationOperation.Result result = store.maintenance().send(operation);

            assertThat(result.getEtag())
                    .isNotNull();

            ClientConfiguration newConfiguration = result.getConfiguration();

            assertThat(newConfiguration)
                    .isNotNull();

            assertThat(newConfiguration.isDisabled())
                    .isTrue();

            assertThat(newConfiguration.getMaxNumberOfRequestsPerSession())
                    .isEqualTo(80);

            assertThat(newConfiguration.getReadBalanceBehavior())
                    .isEqualTo(ReadBalanceBehavior.FASTEST_NODE);
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    public void shouldSwitchToTestPhase_OnClientConfigurationChange_WithReadBalanceBehavior_FastestNode(boolean isServerWide) throws Exception {
        try (DocumentStore store = getDocumentStore()) {
            RequestExecutor requestExecutor = store.getRequestExecutor();

            // Check if we are not in speed test phase and read balance behavior is not fastest node
            boolean isNotReadBalanceBehaviorFastestNode = waitForValue(() -> {
                loadUser(store);
                return requestExecutor.getConventions().getReadBalanceBehavior() != ReadBalanceBehavior.FASTEST_NODE;
            }, true, Duration.ofSeconds(5));

            assertThat(isNotReadBalanceBehaviorFastestNode).isTrue();
            assertThat(requestExecutor.inSpeedTestPhase()).isFalse();
            assertThat(hasUpdateFastestNodeTimer(requestExecutor)).isFalse();

            try {
                ClientConfiguration configuration = new ClientConfiguration();
                configuration.setReadBalanceBehavior(ReadBalanceBehavior.FASTEST_NODE);
                configuration.setDisabled(false);

                putClientConfiguration(store, configuration, isServerWide);

                // Now we should be in speed test phase and read balance behavior should be `FastestNode`
                boolean isReadBalanceBehaviorFastestNode = waitForValue(() -> {
                    loadUser(store);
                    return requestExecutor.getConventions().getReadBalanceBehavior() == ReadBalanceBehavior.FASTEST_NODE
                            && requestExecutor.inSpeedTestPhase();
                }, true, Duration.ofSeconds(5));

                assertThat(isReadBalanceBehaviorFastestNode).isTrue();
                assertThat(requestExecutor.inSpeedTestPhase()).isTrue();
                assertThat(hasUpdateFastestNodeTimer(requestExecutor)).isTrue();
            } finally {
                ClientConfiguration disabled = new ClientConfiguration();
                disabled.setDisabled(true);

                putClientConfiguration(store, disabled, isServerWide);
            }

            // Now we should not be in speed test phase and read balance behavior should be `None`
            isNotReadBalanceBehaviorFastestNode = waitForValue(() -> {
                loadUser(store);
                return requestExecutor.getConventions().getReadBalanceBehavior() == ReadBalanceBehavior.NONE
                        && !requestExecutor.inSpeedTestPhase();
            }, true, Duration.ofSeconds(5));

            assertThat(isNotReadBalanceBehaviorFastestNode).isTrue();
            assertThat(requestExecutor.inSpeedTestPhase()).isFalse();
            assertThat(hasUpdateFastestNodeTimer(requestExecutor)).isFalse();
        }
    }

    private static void loadUser(IDocumentStore store) {
        try (IDocumentSession session = store.openSession()) {
            session.load(User.class, "users/1");
        }
    }

    private static void putClientConfiguration(IDocumentStore store, ClientConfiguration configuration, boolean isServerWide) {
        if (isServerWide) {
            store.maintenance().server().send(new PutServerWideClientConfigurationOperation(configuration));
        } else {
            store.maintenance().send(new PutClientConfigurationOperation(configuration));
        }
    }

    private static boolean hasUpdateFastestNodeTimer(RequestExecutor requestExecutor) throws Exception {
        Field nodeSelectorField = RequestExecutor.class.getDeclaredField("_nodeSelector");
        nodeSelectorField.setAccessible(true);
        NodeSelector nodeSelector = (NodeSelector) nodeSelectorField.get(requestExecutor);

        Field timerField = NodeSelector.class.getDeclaredField("_updateFastestNodeTimer");
        timerField.setAccessible(true);
        return timerField.get(nodeSelector) != null;
    }
}
