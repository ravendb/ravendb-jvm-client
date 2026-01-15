package net.ravendb.client.documents.queries.timeSeries;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import net.ravendb.client.documents.CloseableIterator;
import java.util.Iterator;

public final class TimeSeriesStreamIterator<T> implements Iterator<T> {

    private final CloseableIterator<ObjectNode> outer;
    private final ObjectMapper mapper = new ObjectMapper();
    private final Class<T> clazz;

    private T current;

    public TimeSeriesStreamIterator(
            CloseableIterator<ObjectNode> outer,
            Class<T> clazz) {

        this.outer = outer;
        this.clazz = clazz;
    }

    @Override
    public boolean hasNext() {
        return outer.hasNext();
    }

    @Override
    public T next() {
        JsonNode node = outer.next();
        current = mapper.convertValue(node, clazz);
        return current;
    }

    public T getCurrent() {
        return current;
    }
}

