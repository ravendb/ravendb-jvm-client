package net.ravendb.client.documents.conventions;

import net.ravendb.client.http.ReadBalanceBehavior;
import net.ravendb.client.serverwide.ClientConfiguration;

//TODO: implement me!
public class DocumentConventions {

    public ReadBalanceBehavior getReadBalanceBehavior() {
        return ReadBalanceBehavior.NONE; //TODO:
    }

    public DocumentConventions clone() {
        return this; //TODO:
    }

    public void updateFrom(ClientConfiguration configuration) {
        //TODO:
    }
}
