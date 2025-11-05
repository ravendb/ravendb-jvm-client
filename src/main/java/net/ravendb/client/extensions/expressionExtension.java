package net.ravendb.client.extensions;

import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.util.SerializableFunction;

import java.lang.invoke.SerializedLambda;
import java.lang.reflect.Method;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.Function;

public class expressionExtension {

    /**
     * Turn a lambda like User::getName into "name".
     */
    public static <T, R> String toPropertyPath(SerializableFunction<T, R> lambda,
                                               DocumentConventions conventions,
                                               char propertySeparator,
                                               String collectionSeparator) {
        String property = extractPropertyName(lambda);
        Deque<String> results = new ArrayDeque<>();
        results.push(property);

        StringBuilder builder = new StringBuilder();
        int stackLength = results.size();

        for (int i = 0; i < stackLength; i++) {
            String curValue = results.pop();

            if (curValue.equals("$Value") && i != stackLength - 1) {
                // Dictionary[].$Value.PropertyName => Dictionary[].PropertyName
                if (builder.length() > 0 && builder.charAt(builder.length() - 1) == propertySeparator) {
                    builder.setLength(builder.length() - 1);
                }
                continue;
            }

            builder.append(curValue);
            if (i != stackLength - 1) {
                builder.append(propertySeparator);
            }
        }

        // trim separators
        String result = builder.toString();
        result = result.replace(collectionSeparator, "");  // remove literal "[]."
        result = result.replace(String.valueOf(propertySeparator), ""); // remove separator chars if needed
        return result;
    }

    public static <T, R> String toPropertyPath(SerializableFunction<T, R> lambda, DocumentConventions conventions) {
        return toPropertyPath(lambda, conventions, '.', "[].");
    }

    // --- Helpers ---
    public static <T,R> String extractPropertyName(SerializableFunction<T,R> lambda) {
        try {
            // Serialize lambda to get the method name
            Method writeReplace = lambda.getClass().getDeclaredMethod("writeReplace");
            writeReplace.setAccessible(true);
            SerializedLambda serialized = (SerializedLambda) writeReplace.invoke(lambda);

            String methodName = serialized.getImplMethodName();

            if (methodName.startsWith("get") && methodName.length() > 3) {
                return decapitalize(methodName.substring(3));
            } else if (methodName.startsWith("is") && methodName.length() > 2) {
                return decapitalize(methodName.substring(2));
            } else {
                return methodName; // fallback
            }

        } catch (Exception e) {
            throw new RuntimeException("Failed to extract property name from lambda", e);
        }
    }


    private static String decapitalize(String str) {
        if (str == null || str.isEmpty()) return str;
        return Character.toLowerCase(str.charAt(0)) + str.substring(1);
    }
}

