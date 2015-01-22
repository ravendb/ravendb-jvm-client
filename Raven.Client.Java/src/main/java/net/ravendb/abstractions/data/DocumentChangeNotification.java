package net.ravendb.abstractions.data;

import net.ravendb.abstractions.basic.EventArgs;


public class DocumentChangeNotification extends EventArgs {
  private DocumentChangeTypes type;
  private String id;
  private String collectionName;
  private String typeName;
  private Etag etag;
  private String message;



  @Override
  public String toString() {
    return String.format("%s on %s", type, id);
  }

  /**
   * Type of change that occured on document.
   */
  public DocumentChangeTypes getType() {
    return type;
  }

  /**
   * Type of change that occured on document.
   * @param type
   */
  public void setType(DocumentChangeTypes type) {
    this.type = type;
  }

  /**
   * Document collection name.
   */
  public String getCollectionName() {
    return collectionName;
  }

  /**
   * Document collection name.
   * @param collectionName
   */
  public void setCollectionName(String collectionName) {
    this.collectionName = collectionName;
  }

  /**
   * Document type name.
   */
  public String getTypeName() {
    return typeName;
  }

  /**
   * Document type name.
   * @param typeName
   */
  public void setTypeName(String typeName) {
    this.typeName = typeName;
  }

  /**
   * Identifier of document for which notification was created.
   */
  public String getId() {
    return id;
  }

  /**
   * Identifier of document for which notification was created.
   * @param id
   */
  public void setId(String id) {
    this.id = id;
  }

  /**
   * Document etag.
   */
  public Etag getEtag() {
    return etag;
  }

  /**
   * Document etag.
   * @param etag
   */
  public void setEtag(Etag etag) {
    this.etag = etag;
  }

  /**
   * Notification message.
   */
  public String getMessage() {
    return message;
  }

  /**
   * Notification message.
   * @param message
   */
  public void setMessage(String message) {
    this.message = message;
  }

}
