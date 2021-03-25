package net.ravendb.client.documents.commands.batches;

import net.ravendb.client.primitives.UseSharpEnum;

@UseSharpEnum
public enum CommandType {
    NONE,
    PUT,
    PATCH,
    DELETE,

    CLIENT_ANY_COMMAND,
    CLIENT_MODIFY_DOCUMENT_COMMAND;

    public static CommandType parseCSharpValue(String input) {
        switch (input) {
            case "None":
                return NONE;
            case "PUT":
                return PUT;
            case "PATCH":
                return PATCH;
            case "DELETE":
                return DELETE;
            default:
                throw new IllegalArgumentException("Unable to parse type: " + input);
        }
    }
}
