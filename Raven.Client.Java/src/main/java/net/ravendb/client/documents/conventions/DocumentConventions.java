package net.ravendb.client.documents.conventions;

import net.ravendb.client.http.ReadBalanceBehavior;
import net.ravendb.client.serverwide.ClientConfiguration;

import javax.print.Doc;

//TODO: implement me!
public class DocumentConventions {

    public static DocumentConventions defaultConventions = new DocumentConventions();

    private ReadBalanceBehavior readBalanceBehavior = ReadBalanceBehavior.NONE;

    public void setReadBalanceBehavior(ReadBalanceBehavior readBalanceBehavior) {
        this.readBalanceBehavior = readBalanceBehavior;
    }

    public boolean getDisableTopologyUpdates() {
        return false;
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
}
