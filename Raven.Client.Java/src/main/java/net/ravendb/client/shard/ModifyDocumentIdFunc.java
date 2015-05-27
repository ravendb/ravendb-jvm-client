package net.ravendb.client.shard;

import net.ravendb.abstractions.closure.Function3;
import net.ravendb.client.document.DocumentConvention;

public interface ModifyDocumentIdFunc extends Function3<DocumentConvention, String, String, String> {
  //empty by design
}
