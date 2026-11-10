package net.ravendb.client.test.issues;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import net.ravendb.client.documents.operations.backups.S3Settings;
import net.ravendb.client.documents.operations.backups.S3StorageClass;
import net.ravendb.client.extensions.JsonExtensions;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class RavenDB_23846Test {

    @Test
    public void s3StorageClassIsSerializedWithCSharpNames() throws Exception {
        ObjectMapper mapper = JsonExtensions.getDefaultMapper();

        S3Settings settings = new S3Settings();
        settings.setStorageClass(S3StorageClass.DEEP_ARCHIVE);

        JsonNode json = mapper.valueToTree(settings);
        assertThat(json.get("StorageClass").asText())
                .isEqualTo("DeepArchive");

        settings.setStorageClass(S3StorageClass.STANDARD_INFREQUENT_ACCESS);
        json = mapper.valueToTree(settings);
        assertThat(json.get("StorageClass").asText())
                .isEqualTo("StandardInfrequentAccess");

        ObjectNode serverJson = mapper.createObjectNode();
        serverJson.put("StorageClass", "GlacierInstantRetrieval");
        S3Settings fromServer = mapper.treeToValue(serverJson, S3Settings.class);
        assertThat(fromServer.getStorageClass())
                .isEqualTo(S3StorageClass.GLACIER_INSTANT_RETRIEVAL);
    }
}
