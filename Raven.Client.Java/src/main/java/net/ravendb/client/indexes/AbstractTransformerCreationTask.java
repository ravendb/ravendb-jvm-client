package net.ravendb.client.indexes;

import org.apache.http.client.utils.URIUtils;

import net.ravendb.abstractions.closure.Action2;
import net.ravendb.abstractions.data.HttpMethods;
import net.ravendb.abstractions.indexing.TransformerDefinition;
import net.ravendb.client.IDocumentStore;
import net.ravendb.client.connection.IDatabaseCommands;
import net.ravendb.client.connection.OperationMetadata;
import net.ravendb.client.connection.ServerClient;
import net.ravendb.client.connection.implementation.HttpJsonRequest;
import net.ravendb.client.document.DocumentConvention;
import net.ravendb.client.document.IndexAndTransformerReplicationMode;
import net.ravendb.client.utils.UrlUtils;

/**
 * Base class for creating transformers
 *
 * The naming convention is that underscores in the inherited class names are replaced by slashed
 * For example: Posts_ByName will be saved to Posts/ByName
 */
public abstract class AbstractTransformerCreationTask extends AbstractCommonApiForIndexesAndTransformers {

  private DocumentConvention conventions;
  protected String transformResults;

  /**
   * Generates transformer name from type name replacing all _ with /
   * e.g.
   * if our type if 'Orders_Totals', then index name would be 'Orders/Totals'
   */
  public String getTransformerName() {
    return getClass().getSimpleName().replace('_', '/');
  }

  public DocumentConvention getConventions() {
    return conventions;
  }

  public void setConventions(DocumentConvention convention) {
    this.conventions = convention;
  }

  /**
   * Creates the Transformer definition.
   */
  public TransformerDefinition createTransformerDefinition() {
    TransformerDefinition transformerDefinition = new TransformerDefinition();
    transformerDefinition.setName(getTransformerName());
    if (transformResults == null) {
      throw new IllegalStateException("You must define transformerDefinition");
    }
    transformerDefinition.setTransformResults(transformResults);

    return transformerDefinition;
  }

  public void execute(IDocumentStore store) {
    store.executeTransformer(this);
  }

  public void execute(IDatabaseCommands databaseCommands, DocumentConvention documentConvention) {
    this.conventions = documentConvention;
    final TransformerDefinition transformerDefinition = createTransformerDefinition();
    // This code take advantage on the fact that RavenDB will turn an index PUT
    // to a noop of the index already exists and the stored definition matches
    // the new definition.
    databaseCommands.putTransformer(getTransformerName(), transformerDefinition);

    if (documentConvention.getIndexAndTransformerReplicationMode().contains(IndexAndTransformerReplicationMode.TRANSFORMERS)) {
      replicateTransformerIfNeeded(databaseCommands);
    }
  }

  private void replicateTransformerIfNeeded(IDatabaseCommands databaseCommands) {
    ServerClient serverClient = (ServerClient) databaseCommands;

    String replicateTransformerUrl = String.format("/replication/replicate-transformers?transformerName=%s", UrlUtils.escapeDataString(getTransformerName()));

    HttpJsonRequest replicateTransformerRequest = serverClient.createRequest(HttpMethods.POST, replicateTransformerUrl);
    try {
      replicateTransformerRequest.executeRequest();
    } catch (Exception e) {
      // ignoring errors
    }
  }

}
