package net.ravendb.tests.bundles.replication;

import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;

import net.ravendb.abstractions.data.Attachment;
import net.ravendb.abstractions.json.linq.RavenJObject;
import net.ravendb.abstractions.util.TimeUtils;
import net.ravendb.client.IDocumentStore;
import net.ravendb.client.connection.IDatabaseCommands;
import net.ravendb.client.exceptions.ConflictException;

import org.junit.Assert;
import org.junit.Test;


@SuppressWarnings("deprecation")
public class AttachmentReplicationBugsTest extends ReplicationBase {

  @Test
  public void can_replicate_documents_between_two_external_instances() {

    try (IDocumentStore store1 = createStore();
      IDocumentStore store2 = createStore()) {
      tellFirstInstanceToReplicateToSecondInstance();
      TimeUtils.cleanSleep(1000);
      IDatabaseCommands databaseCommands = store1.getDatabaseCommands();
      int documentCount = 20;
      for (int i = 0; i < documentCount; i++) {
        databaseCommands.putAttachment(i + "", null, new ByteArrayInputStream(new byte[] {(byte) i}), new RavenJObject());
      }

      boolean foundAll = false;
      for (int i = 0; i < retriesCount; i++) {
        int countFound = 0;
        for (int j = 0; j < documentCount; j++) {
          Attachment attachment = store2.getDatabaseCommands().getAttachment(j + "");
          if (attachment == null) break;
          countFound++;
        }
        foundAll = countFound == documentCount;
        if (foundAll) break;
        TimeUtils.cleanSleep(100);
      }
      Assert.assertTrue(foundAll);

    }
  }

  @Test
  public void can_resolve_conflict_with_delete() {

    try (IDocumentStore store1 = createStore();
      IDocumentStore store2 = createStore()) {

      store1.getDatabaseCommands().putAttachment("static", null, new ByteArrayInputStream(new byte[] {(byte) 1}),
        new RavenJObject());
      store2.getDatabaseCommands().putAttachment("static", null, new ByteArrayInputStream(new byte[] {(byte) 2}),
        new RavenJObject());

      tellFirstInstanceToReplicateToSecondInstance();

      try {
        for (int i = 0; i < retriesCount; i++) {
          store2.getDatabaseCommands().getAttachment("static");
          TimeUtils.cleanSleep(100);
        }
        fail();
      } catch (ConflictException e) {
        store2.getDatabaseCommands().deleteAttachment("static", null);

        for (String conflictedVersionId : e.getConflictedVersionIds()) {
          Assert.assertNull(store2.getDatabaseCommands().getAttachment(conflictedVersionId));
        }
      }

    }
  }


}
