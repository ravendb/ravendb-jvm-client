package net.ravendb.client.documents.conventions;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import net.ravendb.client.Constants;
import net.ravendb.client.exceptions.RavenException;
import net.ravendb.client.extensions.JsonExtensions;
import net.ravendb.client.http.ReadBalanceBehavior;
import net.ravendb.client.serverwide.ClientConfiguration;
import org.apache.commons.lang3.NotImplementedException;

import javax.print.Doc;
import java.lang.reflect.Field;

//TODO: implement me!
public class DocumentConventions {

    public static DocumentConventions defaultConventions = new DocumentConventions();

    private ReadBalanceBehavior readBalanceBehavior = ReadBalanceBehavior.NONE;

    public void setReadBalanceBehavior(ReadBalanceBehavior readBalanceBehavior) {
        this.readBalanceBehavior = readBalanceBehavior;
    }

    public String generateDocumentId(String databaseName, Object entity) {
        return entity.getClass().getSimpleName() + "/1";//TODO:
    }

    public boolean getDisableTopologyUpdates() {
        return false;
    }


    public Field getIdentityProperty(Class<?> type) {
        try {
            Field id = type.getDeclaredField("id");
            id.setAccessible(true);
            return id;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public ReadBalanceBehavior getReadBalanceBehavior() {
        return this.readBalanceBehavior;
    }

    public DocumentConventions clone() {
        return this; //TODO:
    }

    public void updateFrom(ClientConfiguration configuration) {
        //TODO:
    }

    public void freeze() {
        //TODO
    }

    public boolean getUseOptimisticConcurrency() {
        return true;
    }

    public int getMaxNumberOfRequestsPerSession() {
        return 50;
    }

    public String getCollectionName(Object entity) {
        return "users"; //TODO:
    }

    public Class getJavaClass(String id, ObjectNode document) {

        JsonNode metadata = document.get(Constants.Documents.Metadata.KEY);
        JsonNode jsonNode = metadata.get(Constants.Documents.Metadata.RAVEN_JAVA_TYPE);

        try {
            return Class.forName(jsonNode.asText());
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public Object deserializeEntityFromJson(Class documentType, ObjectNode document) {
        try {
            return JsonExtensions.getDefaultMapper().treeToValue(document, documentType);
        } catch (JsonProcessingException e) {
            throw new RavenException("Cannot deserialize entity", e);
        }
    }

    public String getJavaClassName(Class<?> aClass) {
        return aClass.getName(); //TODO:
    }
}
