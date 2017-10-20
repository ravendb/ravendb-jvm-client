package net.ravendb.client.documents.conventions;

import net.ravendb.client.http.ReadBalanceBehavior;
import net.ravendb.client.serverwide.ClientConfiguration;

public class DocumentConventions {
    //TODO:

    public ReadBalanceBehavior getReadBalanceBehavior() {
        return ReadBalanceBehavior.NONE; //TODO:
    }

    public DocumentConventions clone() {
        return this; //TODO: fake
    }

    public void updateFrom(ClientConfiguration configuration) {
        //TODO:
    }
}
