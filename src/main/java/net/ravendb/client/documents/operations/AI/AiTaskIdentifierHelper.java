package net.ravendb.client.documents.operations.AI;

import java.text.Normalizer;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

final class AiTaskIdentifierHelper {

    private AiTaskIdentifierHelper() {
    }

    static boolean validateIdentifier(String identifier, List<String> errors) {
        errors.clear();

        if (identifier == null || identifier.trim().isEmpty()) {
            errors.add("Identifier cannot be empty or contain only whitespace;");
            return false;
        }

        String normalized = Normalizer.normalize(identifier, Normalizer.Form.NFD);

        if (!identifier.equals(normalized)) {
            errors.add("Identifier contains diacritical marks or non-ASCII characters;");
        }

        for (char c : identifier.toCharArray()) {
            if (Character.isUpperCase(c)) {
                errors.add("Identifier contains uppercase letters;");
                break;
            }
        }

        Set<Character> invalidChars = new HashSet<>();
        for (char c : identifier.toCharArray()) {
            boolean valid =
                    (c >= 'a' && c <= 'z') ||
                            (c >= '0' && c <= '9') ||
                            c == '-';

            if (!valid) {
                invalidChars.add(c);
            }
        }

        if (!invalidChars.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (char c : invalidChars) {
                sb.append("'").append(c).append("', ");
            }
            if (sb.length() > 2) sb.setLength(sb.length() - 2);

            errors.add("Identifier contains invalid characters: " + sb +
                    ". Only lowercase letters (a-z), numbers (0-9) and hyphens (-) are allowed.");
        }

        if (identifier.contains("--")) {
            errors.add("Identifier contains consecutive hyphens;");
        }

        if (identifier.endsWith("-")) {
            errors.add("Identifier ends with a hyphen;");
        }

        return errors.isEmpty();
    }

    static String generateIdentifier(String input) {
        if (input == null || input.trim().isEmpty()) {
            return null;
        }

        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD);
        StringBuilder result = new StringBuilder();
        boolean lastWasHyphen = false;

        for (char c : normalized.toCharArray()) {

            if ((c >= 'a' && c <= 'z') ||
                    (c >= '0' && c <= '9')) {

                result.append(c);
                lastWasHyphen = false;
            }
            else if (c >= 'A' && c <= 'Z') {
                result.append(Character.toLowerCase(c));
                lastWasHyphen = false;
            }
            else {
                if (!lastWasHyphen && result.length() > 0) {
                    result.append('-');
                    lastWasHyphen = true;
                }
            }
        }

        String finalResult = result.toString().replaceAll("-+$", "");

        if (finalResult.isEmpty()) {
            return "AiConnectionStringIdentifier";
        }

        return finalResult;
    }
}
