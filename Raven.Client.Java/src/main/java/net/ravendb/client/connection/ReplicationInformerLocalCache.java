package net.ravendb.client.connection;

import java.io.File;
import java.util.List;

import net.ravendb.abstractions.data.JsonDocument;
import net.ravendb.abstractions.extensions.JsonExtensions;
import net.ravendb.abstractions.json.linq.RavenJObject;
import net.ravendb.abstractions.json.linq.RavenJToken;
import net.ravendb.abstractions.logging.ILog;
import net.ravendb.abstractions.logging.LogManager;

import net.ravendb.client.document.JsonSerializer;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.map.type.CollectionType;
import org.codehaus.jackson.map.type.SimpleType;
import org.codehaus.jackson.type.JavaType;


public class ReplicationInformerLocalCache {

  private static ILog log = LogManager.getCurrentClassLogger();

  private static String tempDir = System.getProperty("java.io.tmpdir");

  public static void clearReplicationInformationFromLocalCache(String serverHash) {
    try {
      String path = "RavenDB Replication Information For - " + serverHash;
      File file = new File(tempDir, path);
      if (file.exists()) {
        file.delete();
      }
    } catch (Exception e) {
      log.error("Could not clear the persisted replication information", e);
    }
  }

  public static JsonDocument tryLoadReplicationInformationFromLocalCache(String serverHash) {
    JsonDocument result = null;
    try {
      String path = "RavenDB Replication Information For - " + serverHash;
      File file = new File(tempDir, path);
      String fileContent = FileUtils.readFileToString(file);
      if (StringUtils.isBlank(fileContent)) {
        return null;
      }
      result = SerializationHelper.toJsonDocument(RavenJObject.parse(fileContent));
    } catch (Exception e) {
      log.error("Could not understand the persisted replication information", e);
      return null;
    }
    return result;
  }

  public static void trySavingReplicationInformationToLocalCache(String serverHash, JsonDocument document) {
    try {
      String path = "RavenDB Replication Information For - " + serverHash;
      File file = new File(tempDir, path);
      FileUtils.writeStringToFile(file, document.toJson().toString());
    } catch (Exception e) {
      log.error("Could not persist the replication information", e);
    }
  }

  public static List<OperationMetadata> tryLoadClusterNodesFromLocalCache(String serverHash) {
    try {
      String path = "RavenDB Cluster Nodes For - " + serverHash;
      File file = new File(tempDir, path);
      String fileContent = FileUtils.readFileToString(file);
      if (StringUtils.isBlank(fileContent)) {
        return null;
      }
      return JsonExtensions.createDefaultJsonSerializer().readValue(fileContent, CollectionType.construct(List.class, SimpleType.construct(OperationMetadata.class)));
    } catch (Exception e) {
      log.error("Could not understand the persisted cluster nodes", e);
      return null;
    }
  }

  public static void trySavingClusterNodesToLocalCache(String serverHash, List<OperationMetadata> nodes) {
    try {
      String path = "RavenDB Cluster Nodes For - " + serverHash;
      File file = new File(tempDir, path);
      FileUtils.writeStringToFile(file, RavenJObject.fromObject(nodes).toString());
    } catch (Exception e) {
      log.error("Could not persist the cluster nodes", e);
    }
  }
}
