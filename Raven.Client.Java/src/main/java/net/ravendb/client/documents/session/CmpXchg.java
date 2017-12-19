package net.ravendb.client.documents.session;

public class CmpXchg extends MethodCall {
    public static CmpXchg value(String key) {
        CmpXchg cmpXchg = new CmpXchg();
        cmpXchg.args = new Object[] { key };

        return cmpXchg;
    }
}
