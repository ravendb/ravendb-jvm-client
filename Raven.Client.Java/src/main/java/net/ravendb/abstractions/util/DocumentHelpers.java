package net.ravendb.abstractions.util;

import net.ravendb.abstractions.json.linq.RavenJArray;
import net.ravendb.abstractions.json.linq.RavenJObject;
import net.ravendb.abstractions.json.linq.RavenJToken;

import java.util.Map;

public class DocumentHelpers {
    /**
     * Gets rough size of RavenJToken in bytes
     */
    public static long getRoughSize(RavenJToken token) {
        long sum;
        switch (token.getType()) {
            case NONE:
                return 0;
            case OBJECT:
                sum = 2; // {}
                for (Map.Entry<String, RavenJToken> prop: (RavenJObject) token) {
                    sum += prop.getKey().length() + 1; // name:
                    sum += getRoughSize(prop.getValue());
                }
                return sum;
            case ARRAY:
                sum = 2;
                for (RavenJToken prop: (RavenJArray) token) {
                    sum += getRoughSize(prop) + 1;
                }
                return sum;
            case BOOLEAN:
                return token.value(Boolean.class) ? 4 : 5;
            case NULL:
                return 4;
            case INTEGER:
            case STRING:
            case FLOAT:
                return token.value(String.class).length();
            default:
                throw new IllegalArgumentException("Don't know how to compute size of: " + token.getType());
        }
    }
}
