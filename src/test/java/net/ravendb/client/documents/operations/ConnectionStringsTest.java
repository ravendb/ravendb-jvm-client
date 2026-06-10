package net.ravendb.client.documents.operations;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.operations.AI.*;
import net.ravendb.client.documents.operations.backups.FtpSettings;
import net.ravendb.client.documents.operations.connectionStrings.*;
import net.ravendb.client.documents.operations.etl.elasticSearch.ElasticSearchConnectionString;
import net.ravendb.client.documents.operations.etl.olap.OlapConnectionString;
import net.ravendb.client.documents.operations.etl.queue.KafkaConnectionSettings;
import net.ravendb.client.documents.operations.etl.queue.QueueBrokerType;
import net.ravendb.client.documents.operations.etl.queue.QueueConnectionString;
import net.ravendb.client.documents.operations.etl.queue.RabbitMqConnectionSettings;
import net.ravendb.client.documents.operations.etl.sql.SqlConnectionString;
import net.ravendb.client.infrastructure.EnableOnServer;
import net.ravendb.client.serverwide.ConnectionStringType;
import net.ravendb.client.documents.operations.etl.RavenConnectionString;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.EnumSet;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class ConnectionStringsTest extends RemoteTestBase {

    @EnableOnServer(thresholdVersion = "7.1")
    @Test
    public void canCreateGetAndDeleteOpenAiConnectionString() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            AiConnectionString aiConnectionString = new AiConnectionString();
            aiConnectionString.setName("openai1");
            aiConnectionString.setModelType(AiModelType.TextEmbeddings);

            OpenAiSettings openAiSettings = new OpenAiSettings(
                    "test-api-key",
                    "https://api.openai.com/",
                    "text-embedding-ada-002",
                    "org-123",
                    "proj-456",
                    1536,
                    null
            );
            aiConnectionString.setOpenAiSettings(openAiSettings);

            IMaintenanceOperation<PutConnectionStringResult> putOperation = new PutConnectionStringOperation(aiConnectionString);
            PutConnectionStringResult putResult = store.maintenance().send(putOperation);
            assertThat(putResult.getRaftCommandIndex()).isGreaterThan(0);

            GetConnectionStringsOperation getOperation = new GetConnectionStringsOperation();
            GetConnectionStringsResult connectionStrings = store.maintenance().send(getOperation);

            assertThat(connectionStrings.getAiConnectionStrings()).isNotNull();
            assertThat(connectionStrings.getAiConnectionStrings()).hasSize(1);
            assertThat(connectionStrings.getAiConnectionStrings().get("openai1")).isInstanceOf(AiConnectionString.class);

            AiConnectionString retrieved = connectionStrings.getAiConnectionStrings().get("openai1");
            assertThat(retrieved.getName()).isEqualTo("openai1");
            assertThat(retrieved.getModelType()).isEqualTo(AiModelType.TextEmbeddings);
            assertThat(retrieved.getOpenAiSettings()).isNotNull();
            assertThat(retrieved.getOpenAiSettings().getApiKey()).isEqualTo("test-api-key");
            assertThat(retrieved.getOpenAiSettings().getModel()).isEqualTo("text-embedding-ada-002");
            assertThat(retrieved.getOpenAiSettings().getOrganizationId()).isEqualTo("org-123");
            assertThat(retrieved.getOpenAiSettings().getProjectId()).isEqualTo("proj-456");

            RemoveConnectionStringOperation removeOperation = new RemoveConnectionStringOperation(aiConnectionString);
            store.maintenance().send(removeOperation);

            GetConnectionStringsResult afterDelete = store.maintenance().send(new GetConnectionStringsOperation());
            assertThat(afterDelete.getAiConnectionStrings()).hasSize(0);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @EnableOnServer(thresholdVersion = "7.1")
    @Test
    public void canCreateAzureOpenAiConnectionString() {
        try (IDocumentStore store = getDocumentStore()) {
            AiConnectionString aiConnectionString = new AiConnectionString();
            aiConnectionString.setName("azure1");
            aiConnectionString.setModelType(AiModelType.Chat);

            AzureOpenAiSettings azureSettings = new AzureOpenAiSettings(
                    "azure-key",
                    "https://myresource.openai.azure.com/",
                    "gpt-4",
                    "my-deployment",
                    0.7
            );
            aiConnectionString.setAzureOpenAiSettings(azureSettings);

            IMaintenanceOperation<PutConnectionStringResult> putOperation = new PutConnectionStringOperation(aiConnectionString);
            PutConnectionStringResult putResult = store.maintenance().send(putOperation);

            assertThat(putResult.getRaftCommandIndex())
                    .isGreaterThan(0);

            GetConnectionStringsOperation getOperation = new GetConnectionStringsOperation();
            GetConnectionStringsResult connectionStrings = store.maintenance().send(getOperation);

            AiConnectionString retrieved = connectionStrings.getAiConnectionStrings().get("azure1");

            assertThat(retrieved).isNotNull();
            assertThat(retrieved.getAzureOpenAiSettings()).isNotNull();
            assertThat(retrieved.getAzureOpenAiSettings().getDeploymentName()).isEqualTo("my-deployment");
            assertThat(retrieved.getAzureOpenAiSettings().getTemperature()).isEqualTo(0.7);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @EnableOnServer(thresholdVersion = "7.1")
    @Test
    public void canCreateOllamaConnectionString() {
        try (IDocumentStore store = getDocumentStore()) {
            AiConnectionString aiConnectionString = new AiConnectionString();
            aiConnectionString.setName("ollama1");
            aiConnectionString.setModelType(AiModelType.Chat);

            OllamaSettings ollamaSettings = new OllamaSettings(
                    "http://localhost:11434",
                    "llama2"
            );
            ollamaSettings.setThink(true);
            ollamaSettings.setTemperature(0.8);

            aiConnectionString.setOllamaSettings(ollamaSettings);

            IMaintenanceOperation<PutConnectionStringResult> putOperation = new PutConnectionStringOperation(aiConnectionString);
            PutConnectionStringResult putResult = store.maintenance().send(putOperation);

            assertThat(putResult.getRaftCommandIndex())
                    .isGreaterThan(0);

            GetConnectionStringsOperation getOperation = new GetConnectionStringsOperation();
            GetConnectionStringsResult connectionStrings = store.maintenance().send(getOperation);

            AiConnectionString retrieved = connectionStrings.getAiConnectionStrings().get("ollama1");

            assertThat(retrieved).isNotNull();
            assertThat(retrieved.getOllamaSettings()).isNotNull();
            assertThat(retrieved.getOllamaSettings().getUri()).isEqualTo("http://localhost:11434");
            assertThat(retrieved.getOllamaSettings().getModel()).isEqualTo("llama2");
            assertThat(retrieved.getOllamaSettings().getThink()).isTrue();
            assertThat(retrieved.getOllamaSettings().getTemperature()).isEqualTo(0.8);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @EnableOnServer(thresholdVersion = "7.1")
    @Test
    public void canCreateGoogleConnectionString() {
        try (IDocumentStore store = getDocumentStore()) {
            AiConnectionString aiConnectionString = new AiConnectionString();
            aiConnectionString.setName("google1");
            aiConnectionString.setModelType(AiModelType.TextEmbeddings);

            GoogleSettings googleSettings = new GoogleSettings(
                    "text-embedding-004",
                    "google-api-key",
                    GoogleAIVersion.V1,
                    768
            );

            aiConnectionString.setGoogleSettings(googleSettings);

            IMaintenanceOperation<PutConnectionStringResult> putOperation = new PutConnectionStringOperation(aiConnectionString);
            PutConnectionStringResult putResult = store.maintenance().send(putOperation);

            assertThat(putResult.getRaftCommandIndex())
                    .isGreaterThan(0);

            GetConnectionStringsOperation getOperation = new GetConnectionStringsOperation();
            GetConnectionStringsResult connectionStrings = store.maintenance().send(getOperation);

            AiConnectionString retrieved = connectionStrings.getAiConnectionStrings().get("google1");

            assertThat(retrieved).isNotNull();
            assertThat(retrieved.getGoogleSettings()).isNotNull();
            assertThat(retrieved.getGoogleSettings().getModel()).isEqualTo("text-embedding-004");
            assertThat(retrieved.getGoogleSettings().getAiVersion()).isEqualTo(GoogleAIVersion.V1);
            assertThat(retrieved.getGoogleSettings().getDimensions()).isEqualTo(768);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @EnableOnServer(thresholdVersion = "7.1")
    @Test
    public void canCreateHuggingFaceConnectionString() {
        try (IDocumentStore store = getDocumentStore()) {
            AiConnectionString aiConnectionString = new AiConnectionString();
            aiConnectionString.setName("huggingface1");
            aiConnectionString.setModelType(AiModelType.TextEmbeddings);

            HuggingFaceSettings huggingFaceSettings = new HuggingFaceSettings(
                    "hf-api-key",
                    "sentence-transformers/all-MiniLM-L6-v2",
                    "https://api-inference.huggingface.co"
            );
            aiConnectionString.setHuggingFaceSettings(huggingFaceSettings);

            IMaintenanceOperation<PutConnectionStringResult> putOperation = new PutConnectionStringOperation(aiConnectionString);
            PutConnectionStringResult putResult = store.maintenance().send(putOperation);

            assertThat(putResult.getRaftCommandIndex())
                    .isGreaterThan(0);

            GetConnectionStringsResult connectionStrings = store.maintenance().send(new GetConnectionStringsOperation());
            AiConnectionString retrieved = connectionStrings.getAiConnectionStrings().get("huggingface1");

            assertThat(retrieved).isNotNull();
            assertThat(retrieved.getHuggingFaceSettings()).isNotNull();
            assertThat(retrieved.getHuggingFaceSettings().getModel())
                    .isEqualTo("sentence-transformers/all-MiniLM-L6-v2");
            assertThat(retrieved.getHuggingFaceSettings().getEndpoint())
                    .isEqualTo("https://api-inference.huggingface.co");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @EnableOnServer(thresholdVersion = "7.1")
    @Test
    public void canCreateMistralAiConnectionString() {
        try (IDocumentStore store = getDocumentStore()) {
            AiConnectionString aiConnectionString = new AiConnectionString();
            aiConnectionString.setName("mistral1");
            aiConnectionString.setModelType(AiModelType.Chat);

            MistralAiSettings mistralAiSettings = new MistralAiSettings(
                    "mistral-large-latest",
                    "mistral-api-key",
                    "https://api.mistral.ai"

            );
            aiConnectionString.setMistralAiSettings(mistralAiSettings);

            IMaintenanceOperation<PutConnectionStringResult> putOperation = new PutConnectionStringOperation(aiConnectionString);
            PutConnectionStringResult putResult = store.maintenance().send(putOperation);

            assertThat(putResult.getRaftCommandIndex())
                    .isGreaterThan(0);

            GetConnectionStringsResult connectionStrings = store.maintenance().send(new GetConnectionStringsOperation());
            AiConnectionString retrieved = connectionStrings.getAiConnectionStrings().get("mistral1");

            assertThat(retrieved).isNotNull();
            assertThat(retrieved.getMistralAiSettings()).isNotNull();
            assertThat(retrieved.getMistralAiSettings().getModel()).isEqualTo("mistral-large-latest");
            assertThat(retrieved.getMistralAiSettings().getEndpoint()).isEqualTo("https://api.mistral.ai");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @EnableOnServer(thresholdVersion = "7.1")
    @Test
    public void canCreateVertexConnectionString() {
        try (IDocumentStore store = getDocumentStore()) {
            AiConnectionString aiConnectionString = new AiConnectionString();
            aiConnectionString.setName("vertex1");
            aiConnectionString.setModelType(AiModelType.TextEmbeddings);

            String credentialsJson =
                    "{\n" +
                            "  \"type\": \"service_account\",\n" +
                            "  \"project_id\": \"my-project-123\",\n" +
                            "  \"private_key_id\": \"key-id\",\n" +
                            "  \"private_key\": \"-----BEGIN PRIVATE KEY-----\\ntest\\n-----END PRIVATE KEY-----\\n\",\n" +
                            "  \"client_email\": \"test@my-project.iam.gserviceaccount.com\",\n" +
                            "  \"client_id\": \"123456\",\n" +
                            "  \"auth_uri\": \"https://accounts.google.com/o/oauth2/auth\",\n" +
                            "  \"token_uri\": \"https://oauth2.googleapis.com/token\"\n" +
                            "}";

            VertexSettings vertexSettings = new VertexSettings(
                    "text-embedding-004",
                    credentialsJson,
                    "us-central1",
                    VertexAIVersion.V1
            );
            aiConnectionString.setVertexSettings(vertexSettings);

            IMaintenanceOperation<PutConnectionStringResult> putOperation = new PutConnectionStringOperation(aiConnectionString);
            PutConnectionStringResult putResult = store.maintenance().send(putOperation);

            assertThat(putResult.getRaftCommandIndex())
                    .isGreaterThan(0);

            GetConnectionStringsResult connectionStrings = store.maintenance().send(new GetConnectionStringsOperation());
            AiConnectionString retrieved = connectionStrings.getAiConnectionStrings().get("vertex1");

            assertThat(retrieved).isNotNull();
            assertThat(retrieved.getVertexSettings()).isNotNull();
            assertThat(retrieved.getVertexSettings().getModel()).isEqualTo("text-embedding-004");
            assertThat(retrieved.getVertexSettings().getLocation()).isEqualTo("us-central1");
            assertThat(retrieved.getVertexSettings().getAiVersion()).isEqualTo(VertexAIVersion.V1);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @EnableOnServer(thresholdVersion = "7.1")
    @Test
    public void canGetMultipleAiConnectionStrings() {
        try (IDocumentStore store = getDocumentStore()) {
            OpenAiSettings openAiSettings = new OpenAiSettings();
            openAiSettings.setApiKey("key1");
            openAiSettings.setEndpoint("https://api.openai.com/");
            openAiSettings.setModel("model1");

            AiConnectionString openAiCs = new AiConnectionString();
            openAiCs.setName("openai");
            openAiCs.setOpenAiSettings(openAiSettings);

            OllamaSettings ollamaSettings = new OllamaSettings();
            ollamaSettings.setUri("http://localhost:11434");
            ollamaSettings.setModel("llama2");

            AiConnectionString ollamaCs = new AiConnectionString();
            ollamaCs.setName("ollama");
            ollamaCs.setOllamaSettings(ollamaSettings);

            IMaintenanceOperation<PutConnectionStringResult> putOpenAi = new PutConnectionStringOperation(openAiCs);
            IMaintenanceOperation<PutConnectionStringResult> putOllama = new PutConnectionStringOperation(ollamaCs);

            store.maintenance().send(putOpenAi);
            store.maintenance().send(putOllama);

            GetConnectionStringsResult connectionStrings = store.maintenance().send(new GetConnectionStringsOperation());

            assertThat(connectionStrings.getAiConnectionStrings()).hasSize(2);
            assertThat(connectionStrings.getAiConnectionStrings().get("openai")).isNotNull();
            assertThat(connectionStrings.getAiConnectionStrings().get("ollama")).isNotNull();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @EnableOnServer(thresholdVersion = "7.1")
    @Test
    public void canGetSpecificAiConnectionString() {
        try (IDocumentStore store = getDocumentStore()) {
            OpenAiSettings openAiSettings = new OpenAiSettings();
            openAiSettings.setApiKey("key");
            openAiSettings.setEndpoint("https://api.openai.com/");
            openAiSettings.setModel("model");

            AiConnectionString aiConnectionString = new AiConnectionString();
            aiConnectionString.setName("specific");
            aiConnectionString.setOpenAiSettings(openAiSettings);

            IMaintenanceOperation<PutConnectionStringResult> putOperation = new PutConnectionStringOperation(aiConnectionString);
            store.maintenance().send(putOperation);

            GetConnectionStringsOperation getOperation = new GetConnectionStringsOperation("specific", ConnectionStringType.AI);
            GetConnectionStringsResult result = store.maintenance().send(getOperation);

            assertThat(result.getAiConnectionStrings()).hasSize(1);
            assertThat(result.getAiConnectionStrings().get("specific")).isNotNull();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @EnableOnServer(thresholdVersion = "7.1")
    @Test
    public void validationFailsWhenNoProviderConfigured() {
        AiConnectionString aiConnectionString = new AiConnectionString();
        aiConnectionString.setName("invalid");

        List<String> errors = aiConnectionString.validate();

        assertThat(errors).isNotEmpty();
        assertThat(errors.get(0)).contains("At least one of the following settings must be set");
    }

    @EnableOnServer(thresholdVersion = "7.1")
    @Test
    public void validationFailsWhenMultipleProvidersConfigured() {
        OpenAiSettings openAiSettings = new OpenAiSettings();
        openAiSettings.setApiKey("key1");
        openAiSettings.setEndpoint("https://api.openai.com/");
        openAiSettings.setModel("model1");

        OllamaSettings ollamaSettings = new OllamaSettings();
        ollamaSettings.setUri("http://localhost:11434");
        ollamaSettings.setModel("llama2");

        AiConnectionString aiConnectionString = new AiConnectionString();
        aiConnectionString.setName("invalid");
        aiConnectionString.setOpenAiSettings(openAiSettings);
        aiConnectionString.setOllamaSettings(ollamaSettings);

        List<String> errors = aiConnectionString.validate();

        assertThat(errors).isNotEmpty();
        assertThat(errors.get(0)).contains("Only one of the following settings can be set");
    }

    @EnableOnServer(thresholdVersion = "7.1")
    @Test
    public void validationFailsWhenRequiredFieldsMissing() {
        OpenAiSettings openAiSettings = new OpenAiSettings();
        openAiSettings.setApiKey("");
        openAiSettings.setEndpoint("");
        openAiSettings.setModel("");

        AiConnectionString aiConnectionString = new AiConnectionString();
        aiConnectionString.setName("invalid");
        aiConnectionString.setOpenAiSettings(openAiSettings);

        List<String> errors = aiConnectionString.validate();

        assertThat(errors.size()).isGreaterThan(0);
        assertThat(errors.stream().anyMatch(e -> e.contains("apiKey"))).isTrue();
        assertThat(errors.stream().anyMatch(e -> e.contains("model"))).isTrue();
        // endpoint is no longer required: when blank it defaults to https://api.openai.com/v1/
        assertThat(errors.stream().anyMatch(e -> e.contains("endpoint"))).isFalse();
    }

    @EnableOnServer(thresholdVersion = "7.1")
    @Test
    public void getActiveProviderReturnsCorrectType() {
        OpenAiSettings openAiSettings = new OpenAiSettings();
        openAiSettings.setApiKey("key");
        openAiSettings.setEndpoint("https://api.openai.com/");
        openAiSettings.setModel("model");

        AiConnectionString cs1 = new AiConnectionString();
        cs1.setOpenAiSettings(openAiSettings);
        assertThat(cs1.getActiveProvider()).isEqualTo(AiConnectorType.OpenAi);

        AzureOpenAiSettings azureSettings = new AzureOpenAiSettings();
        azureSettings.setApiKey("key");
        azureSettings.setEndpoint("endpoint");
        azureSettings.setModel("model");
        azureSettings.setDeploymentName("deployment");

        AiConnectionString cs2 = new AiConnectionString();
        cs2.setAzureOpenAiSettings(azureSettings);
        assertThat(cs2.getActiveProvider()).isEqualTo(AiConnectorType.AzureOpenAi);

        OllamaSettings ollamaSettings = new OllamaSettings();
        ollamaSettings.setUri("http://localhost:11434");
        ollamaSettings.setModel("llama2");

        AiConnectionString cs3 = new AiConnectionString();
        cs3.setOllamaSettings(ollamaSettings);
        assertThat(cs3.getActiveProvider()).isEqualTo(AiConnectorType.Ollama);

        AiConnectionString cs4 = new AiConnectionString();
        assertThat(cs4.getActiveProvider()).isEqualTo(AiConnectorType.None);
    }

    @EnableOnServer(thresholdVersion = "7.1")
    @Test
    public void compareDetectsModelChanges() {
        OpenAiSettings settings1 = new OpenAiSettings();
        settings1.setApiKey("key");
        settings1.setEndpoint("https://api.openai.com/");
        settings1.setModel("model1");

        OpenAiSettings settings2 = new OpenAiSettings();
        settings2.setApiKey("key");
        settings2.setEndpoint("https://api.openai.com/");
        settings2.setModel("model2");

        AiConnectionString cs1 = new AiConnectionString();
        cs1.setOpenAiSettings(settings1);

        AiConnectionString cs2 = new AiConnectionString();
        cs2.setOpenAiSettings(settings2);

        EnumSet<AiSettingsCompareDifferences> diff = cs1.compare(cs2);

        assertThat(diff).contains(AiSettingsCompareDifferences.ModelArchitecture);
        assertThat(diff).doesNotContain(AiSettingsCompareDifferences.None);
    }

    @EnableOnServer(thresholdVersion = "7.1")
    @Test
    public void compareDetectsEndpointChanges() {
        OpenAiSettings settings1 = new OpenAiSettings();
        settings1.setApiKey("key");
        settings1.setEndpoint("https://api1.openai.com/");
        settings1.setModel("model");

        OpenAiSettings settings2 = new OpenAiSettings();
        settings2.setApiKey("key");
        settings2.setEndpoint("https://api2.openai.com/");
        settings2.setModel("model");

        AiConnectionString cs1 = new AiConnectionString();
        cs1.setOpenAiSettings(settings1);

        AiConnectionString cs2 = new AiConnectionString();
        cs2.setOpenAiSettings(settings2);

        EnumSet<AiSettingsCompareDifferences> diff = cs1.compare(cs2);

        assertThat(diff).contains(AiSettingsCompareDifferences.EndpointConfiguration);
    }

    @EnableOnServer(thresholdVersion = "7.1")
    @Test
    public void compareDetectsAuthenticationChanges() {
        OpenAiSettings settings1 = new OpenAiSettings();
        settings1.setApiKey("key1");
        settings1.setEndpoint("https://api.openai.com/");
        settings1.setModel("model");

        OpenAiSettings settings2 = new OpenAiSettings();
        settings2.setApiKey("key2");
        settings2.setEndpoint("https://api.openai.com/");
        settings2.setModel("model");

        AiConnectionString cs1 = new AiConnectionString();
        cs1.setOpenAiSettings(settings1);

        AiConnectionString cs2 = new AiConnectionString();
        cs2.setOpenAiSettings(settings2);

        EnumSet<AiSettingsCompareDifferences> diff = cs1.compare(cs2);

        assertThat(diff).contains(AiSettingsCompareDifferences.AuthenticationSettings);
    }

    @EnableOnServer(thresholdVersion = "7.1")
    @Test
    public void compareDetectsProviderChange() {
        OpenAiSettings openAiSettings = new OpenAiSettings();
        openAiSettings.setApiKey("key");
        openAiSettings.setEndpoint("https://api.openai.com/");
        openAiSettings.setModel("model");

        OllamaSettings ollamaSettings = new OllamaSettings(
                "http://localhost:11434",
                "llama2"
        );

        AiConnectionString cs1 = new AiConnectionString();
        cs1.setOpenAiSettings(openAiSettings);

        AiConnectionString cs2 = new AiConnectionString();
        cs2.setOllamaSettings(ollamaSettings);

        EnumSet<AiSettingsCompareDifferences> diff = cs1.compare(cs2);

        assertThat(diff).isEqualTo(EnumSet.of(AiSettingsCompareDifferences.All));
    }

    @EnableOnServer(thresholdVersion = "7.1")
    @Test
    public void compareReturnsNoneWhenIdentical() {
        OpenAiSettings settings1 = new OpenAiSettings();
        settings1.setApiKey("key");
        settings1.setEndpoint("https://api.openai.com/");
        settings1.setModel("model");

        OpenAiSettings settings2 = new OpenAiSettings();
        settings2.setApiKey("key");
        settings2.setEndpoint("https://api.openai.com/");
        settings2.setModel("model");

        AiConnectionString cs1 = new AiConnectionString();
        cs1.setIdentifier("id1");
        cs1.setModelType(AiModelType.Chat);
        cs1.setOpenAiSettings(settings1);

        AiConnectionString cs2 = new AiConnectionString();
        cs2.setIdentifier("id1");
        cs2.setModelType(AiModelType.Chat);
        cs2.setOpenAiSettings(settings2);

        EnumSet<AiSettingsCompareDifferences> diff = cs1.compare(cs2);

        assertThat(diff).isEqualTo(EnumSet.of(AiSettingsCompareDifferences.None));
    }

    @EnableOnServer(thresholdVersion = "7.1")
    @Test
    public void isEqualReturnsTrueForIdenticalConnectionStrings() {
        OpenAiSettings settings1 = new OpenAiSettings();
        settings1.setApiKey("key");
        settings1.setEndpoint("https://api.openai.com/");
        settings1.setModel("model");

        OpenAiSettings settings2 = new OpenAiSettings();
        settings2.setApiKey("key");
        settings2.setEndpoint("https://api.openai.com/");
        settings2.setModel("model");

        AiConnectionString cs1 = new AiConnectionString();
        cs1.setName("test");
        cs1.setIdentifier("id1");
        cs1.setModelType(AiModelType.Chat);
        cs1.setOpenAiSettings(settings1);

        AiConnectionString cs2 = new AiConnectionString();
        cs2.setName("test");
        cs2.setIdentifier("id1");
        cs2.setModelType(AiModelType.Chat);
        cs2.setOpenAiSettings(settings2);

        assertThat(cs1.isEqual(cs2)).isTrue();
    }

    @EnableOnServer(thresholdVersion = "7.1")
    @Test
    public void isEqualReturnsFalseForDifferentNames() {
        OpenAiSettings settings1 = new OpenAiSettings();
        settings1.setApiKey("key");
        settings1.setEndpoint("https://api.openai.com/");
        settings1.setModel("model");

        OpenAiSettings settings2 = new OpenAiSettings();
        settings2.setApiKey("key");
        settings2.setEndpoint("https://api.openai.com/");
        settings2.setModel("model");

        AiConnectionString cs1 = new AiConnectionString();
        cs1.setName("test1");
        cs1.setOpenAiSettings(settings1);

        AiConnectionString cs2 = new AiConnectionString();
        cs2.setName("test2");
        cs2.setOpenAiSettings(settings2);

        assertThat(cs1.isEqual(cs2)).isFalse();
    }

    @EnableOnServer(thresholdVersion = "7.1")
    @Test
    public void usingEncryptedCommunicationChannelDetectsHttps() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        OpenAiSettings openAiSettings = new OpenAiSettings();
        openAiSettings.setApiKey("key");
        openAiSettings.setEndpoint("https://api.openai.com/");
        openAiSettings.setModel("model");

        AiConnectionString cs1 = new AiConnectionString();
        cs1.setOpenAiSettings(openAiSettings);

        Method method = AiConnectionString.class.getDeclaredMethod("usingEncryptedCommunicationChannel");
        method.setAccessible(true);
        boolean result = (boolean) method.invoke(cs1);
        assertThat(result).isTrue();

        OllamaSettings ollamaSettings1 = new OllamaSettings();
        ollamaSettings1.setUri("http://localhost:11434");
        ollamaSettings1.setModel("llama2");

        AiConnectionString cs2 = new AiConnectionString();
        cs2.setOllamaSettings(ollamaSettings1);

        Method method2 = AiConnectionString.class.getDeclaredMethod("usingEncryptedCommunicationChannel");
        method2.setAccessible(true);
        boolean result2 = (boolean) method.invoke(cs2);
        assertThat(result2).isFalse();

        OllamaSettings ollamaSettings2 = new OllamaSettings();
        ollamaSettings2.setUri("https://secure-ollama.com");
        ollamaSettings2.setModel("llama2");

        AiConnectionString cs3 = new AiConnectionString();
        cs3.setOllamaSettings(ollamaSettings2);

        Method method3 = AiConnectionString.class.getDeclaredMethod("usingEncryptedCommunicationChannel");
        method3.setAccessible(true);
        boolean result3 = (boolean) method.invoke(cs3);
        assertThat(result3).isTrue();
    }

    @EnableOnServer(thresholdVersion = "7.1")
    @Test
    public void getQueryEmbeddingsMaxConcurrentBatchesUsesProviderValue() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        OpenAiSettings settings = new OpenAiSettings();
        settings.setApiKey("key");
        settings.setEndpoint("https://api.openai.com/");
        settings.setModel("model");
        settings.setEmbeddingsMaxConcurrentBatches(5);

        AiConnectionString cs = new AiConnectionString();
        cs.setOpenAiSettings(settings);

        Method method = AiConnectionString.class.getDeclaredMethod("getQueryEmbeddingsMaxConcurrentBatches", int.class);
        method.setAccessible(true);
        int result = (int) method.invoke(cs, 10);
        assertThat(result).isEqualTo(5);
    }

    @EnableOnServer(thresholdVersion = "7.1")
    @Test
    public void getQueryEmbeddingsMaxConcurrentBatchesUsesGlobalValueWhenNotSet() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        OpenAiSettings settings = new OpenAiSettings();
        settings.setApiKey("key");
        settings.setEndpoint("https://api.openai.com/");
        settings.setModel("model");
        AiConnectionString cs = new AiConnectionString();
        cs.setOpenAiSettings(settings);
        int fallbackValue = 10;

        Method method = AiConnectionString.class.getDeclaredMethod("getQueryEmbeddingsMaxConcurrentBatches", int.class);
        method.setAccessible(true);
        int result = (int) method.invoke(cs, fallbackValue);
        assertThat(result).isEqualTo(10);
    }

    @EnableOnServer(thresholdVersion = "7.1")
    @Test
    public void temperatureParameterSerializedCorrectly() {
        try (IDocumentStore store = getDocumentStore()) {
            OpenAiSettings settings = new OpenAiSettings();
            settings.setApiKey("key");
            settings.setEndpoint("https://api.openai.com/");
            settings.setModel("model");
            settings.setTemperature(1.5);

            AiConnectionString aiConnectionString = new AiConnectionString();
            aiConnectionString.setName("temp-test");
            aiConnectionString.setOpenAiSettings(settings);

            store.maintenance().send(new PutConnectionStringOperation(aiConnectionString));
            GetConnectionStringsResult connectionStrings = store.maintenance().send(new GetConnectionStringsOperation());
            AiConnectionString retrieved = connectionStrings.getAiConnectionStrings().get("temp-test");

            assertThat(retrieved.getOpenAiSettings().getTemperature()).isEqualTo(1.5);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @EnableOnServer(thresholdVersion = "7.1")
    @Test
    public void vertexSettingsCanExtractProjectId() {
        String credentialsJson = "{"
                + "\"type\": \"service_account\","
                + "\"project_id\": \"my-test-project\","
                + "\"private_key_id\": \"key-id\","
                + "\"private_key\": \"test-key\""
                + "}";
        VertexSettings settings = new VertexSettings(
                "text-embedding-004",
                credentialsJson,
                "us-central1"
        );

        assertThat(settings.getProjectId()).isEqualTo("my-test-project");
    }

    @EnableOnServer(thresholdVersion = "7.1")
    @Test
    public void vertexSettingsThrowsWhenProjectIdMissing() {
        String credentialsJson = "{"
                + "\"type\": \"service_account\","
                + "\"private_key_id\": \"key-id\""
                + "}";

        VertexSettings settings = new VertexSettings(
                "text-embedding-004",
                credentialsJson,
                "us-central1"
        );

        boolean errorThrown = false;
        try {
            settings.getProjectId();
        } catch (Exception e) {
            errorThrown = true;
        }

        assertThat(errorThrown).isTrue();
    }


    @Test
    public void canCreateGetAndDeleteConnectionStrings() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {

            RavenConnectionString ravenConnectionString1 = new RavenConnectionString();
            ravenConnectionString1.setDatabase("db1");
            ravenConnectionString1.setTopologyDiscoveryUrls(new String[] { "http://localhost:8080" });
            ravenConnectionString1.setName("r1");

            SqlConnectionString sqlConnectionString1 = new SqlConnectionString();
            sqlConnectionString1.setFactoryName("MySql.Data.MySqlClient");
            sqlConnectionString1.setConnectionString("test");
            sqlConnectionString1.setName("s1");

            ElasticSearchConnectionString elasticSearchConnectionString = new ElasticSearchConnectionString();
            elasticSearchConnectionString.setName("e1");
            elasticSearchConnectionString.setNodes(new String[] { "http://127.0.0.1:8080" });

            QueueConnectionString kafkaConnectionString = new QueueConnectionString();
            kafkaConnectionString.setName("k1");
            kafkaConnectionString.setBrokerType(QueueBrokerType.KAFKA);
            kafkaConnectionString.setKafkaConnectionSettings(new KafkaConnectionSettings());
            kafkaConnectionString.getKafkaConnectionSettings().setBootstrapServers("localhost:9092");

            QueueConnectionString rabbitConnectionString = new QueueConnectionString();
            rabbitConnectionString.setName("r1");
            rabbitConnectionString.setBrokerType(QueueBrokerType.RABBIT_MQ);
            rabbitConnectionString.setRabbitMqConnectionSettings(new RabbitMqConnectionSettings());
            rabbitConnectionString.getRabbitMqConnectionSettings().setConnectionString("localhost:888");

            OlapConnectionString olapConnectionString = new OlapConnectionString();
            olapConnectionString.setName("o1");
            olapConnectionString.setFtpSettings(new FtpSettings());
            olapConnectionString.getFtpSettings().setUrl("localhost:9090");

            PutConnectionStringResult putResult = store.maintenance().send(new PutConnectionStringOperation<>(ravenConnectionString1));
            assertThat(putResult.getRaftCommandIndex())
                    .isPositive();

            putResult = store.maintenance().send(new PutConnectionStringOperation<>(sqlConnectionString1));
            assertThat(putResult.getRaftCommandIndex())
                    .isPositive();

            putResult = store.maintenance().send(new PutConnectionStringOperation<>(elasticSearchConnectionString));
            assertThat(putResult.getRaftCommandIndex())
                    .isPositive();

            putResult = store.maintenance().send(new PutConnectionStringOperation<>(kafkaConnectionString));
            assertThat(putResult.getRaftCommandIndex())
                    .isPositive();

            putResult = store.maintenance().send(new PutConnectionStringOperation<>(rabbitConnectionString));
            assertThat(putResult.getRaftCommandIndex())
                    .isPositive();

            putResult = store.maintenance().send(new PutConnectionStringOperation<>(olapConnectionString));
            assertThat(putResult.getRaftCommandIndex())
                    .isPositive();

            GetConnectionStringsResult connectionStrings = store.maintenance().send(new GetConnectionStringsOperation());
            assertThat(connectionStrings.getRavenConnectionStrings())
                    .containsKey("r1")
                    .hasSize(1);

            assertThat(connectionStrings.getSqlConnectionStrings())
                    .containsKey("s1")
                    .hasSize(1);

            assertThat(connectionStrings.getElasticSearchConnectionStrings())
                    .containsKey("e1")
                    .hasSize(1);

            assertThat(connectionStrings.getOlapConnectionStrings())
                    .containsKey("o1")
                    .hasSize(1);

            assertThat(connectionStrings.getQueueConnectionStrings())
                    .containsKey("k1")
                    .containsKey("r1")
                    .hasSize(2);

            GetConnectionStringsResult ravenOnly = store.maintenance().send(new GetConnectionStringsOperation("r1", ConnectionStringType.RAVEN));
            assertThat(ravenOnly.getRavenConnectionStrings())
                    .containsKey("r1")
                    .hasSize(1);
            assertThat(ravenOnly.getSqlConnectionStrings())
                    .isNullOrEmpty();

            GetConnectionStringsResult sqlOnly = store.maintenance().send(new GetConnectionStringsOperation("s1", ConnectionStringType.SQL));
            assertThat(sqlOnly.getRavenConnectionStrings())
                    .isNullOrEmpty();
            assertThat(sqlOnly.getSqlConnectionStrings())
                    .containsKey("s1")
                    .hasSize(1);

            GetConnectionStringsResult elasticOnly = store.maintenance().send(new GetConnectionStringsOperation("e1", ConnectionStringType.ELASTIC_SEARCH));
            assertThat(elasticOnly.getRavenConnectionStrings())
                    .isNullOrEmpty();
            assertThat(elasticOnly.getElasticSearchConnectionStrings())
                    .containsKey("e1")
                    .hasSize(1);

            GetConnectionStringsResult olapOnly = store.maintenance().send(new GetConnectionStringsOperation("o1", ConnectionStringType.OLAP));
            assertThat(olapOnly.getRavenConnectionStrings())
                    .isNullOrEmpty();
            assertThat(olapOnly.getOlapConnectionStrings())
                    .containsKey("o1")
                    .hasSize(1);

            GetConnectionStringsResult rabbitOnly = store.maintenance().send(new GetConnectionStringsOperation("r1", ConnectionStringType.QUEUE));
            assertThat(rabbitOnly.getRavenConnectionStrings())
                    .isNullOrEmpty();
            assertThat(rabbitOnly.getQueueConnectionStrings())
                    .containsKey("r1")
                    .hasSize(1);

            GetConnectionStringsResult kafkaOnly = store.maintenance().send(new GetConnectionStringsOperation("k1", ConnectionStringType.QUEUE));
            assertThat(kafkaOnly.getRavenConnectionStrings())
                    .isNullOrEmpty();
            assertThat(kafkaOnly.getQueueConnectionStrings())
                    .containsKey("k1")
                    .hasSize(1);


            RemoveConnectionStringResult removeResult = store.maintenance().send(
                    new RemoveConnectionStringOperation<>(sqlOnly.getSqlConnectionStrings().values().stream().findFirst().get()));
            assertThat(removeResult.getRaftCommandIndex())
                    .isPositive();

            GetConnectionStringsResult afterDelete = store.maintenance().send(new GetConnectionStringsOperation("s1", ConnectionStringType.SQL));
            assertThat(afterDelete.getRavenConnectionStrings())
                    .isNullOrEmpty();
            assertThat(afterDelete.getSqlConnectionStrings())
                    .isNullOrEmpty();
        }
    }
}
