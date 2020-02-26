package net.ravendb.client.util;

import java.util.UUID;

public class RaftIdGenerator {
    private RaftIdGenerator() {
        // empty by design
    }

    public static String newId() {
        return UUID.randomUUID().toString();
    }

    // if the don't care id is used it may cause that on retry/resend of the command we will end up in double applying of the command (once for the original request and for the retry).
    public static String dontCareId() {
        return "";
    }
}
