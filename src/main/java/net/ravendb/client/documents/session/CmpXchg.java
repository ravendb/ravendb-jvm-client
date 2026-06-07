package net.ravendb.client.documents.session;

public class CmpXchg extends MethodCall {
    /**
     * Enables the construction of RQL query that specifically retrieve compare exchange values for a given key.
     *
     * @param key The key of the compare exchange.
     * @return cmpxchg method call
     * @deprecated This method is deprecated and will be removed in a future version. Use RavenDocumentQuery.cmpXchg() instead.
     */
    @Deprecated
    public static CmpXchg value(String key) {
        CmpXchg cmpXchg = new CmpXchg();
        cmpXchg.args = new Object[] { key };

        return cmpXchg;
    }
}
