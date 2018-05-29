package net.ravendb.client.documents.session;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class EnumerableUtils {

    /**
     * Extracts list from iterable
     * @param <T> item class
     * @param iterator Iterator to use
     * @return Extracted list
     */
    public static <T> List<T> toList(Iterator<T> iterator) {
        List<T> result = new ArrayList<>();
        while (iterator.hasNext()) {
            result.add(iterator.next());
        }
        return result;
    }

}
