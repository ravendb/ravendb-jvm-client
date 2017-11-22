package net.ravendb.client.documents.queries;

import net.ravendb.client.Parameters;
import org.apache.commons.codec.digest.DigestUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class QueryHashCalculator {

    private ByteArrayOutputStream _buffer = new ByteArrayOutputStream();

    public String getHash() {
        return DigestUtils.md5Hex(_buffer.toByteArray());
    }

    public void write(float f) throws IOException {
        _buffer.write(ByteBuffer.allocate(Float.BYTES).putFloat(f).array());
    }

    public void write(long l) throws IOException {
        _buffer.write(ByteBuffer.allocate(Long.BYTES).putLong(l).array());
    }

    public void write(Long l) throws IOException {
        if (l == null) {
            return;
        }
        write(l.longValue());
    }

    public void write(Float f) throws IOException {
        if (f == null) {
            return;
        }
        write(f.floatValue());
    }

    public void write(Integer i) throws IOException {
        if (i == null) {
            return;
        }
        write(i.intValue());
    }

    public void write(int i) throws IOException {
        _buffer.write(ByteBuffer.allocate(Integer.BYTES).putInt(i).array());
    }

    public void write(Boolean b) {
        if (b == null) {
            return;
        }

        write(b.booleanValue());
    }

    public void write(boolean b) {
        _buffer.write(b ? 1 : 2);
    }

    public void write(String s) throws IOException {
        if (s == null) {
            return;
        }

        _buffer.write(s.getBytes());
    }

    public void write(String[] s) throws IOException {
        if (s == null) {
            return;
        }
        for (int i = 0; i < s.length; i++) {
            write(s[i]);
        }
    }

    public void write(List<String> s) throws IOException {
        if (s == null) {
            return;
        }

        for (int i = 0; i < s.size(); i++) {
            write(s.get(i));
        }
    }

    //TBD public void Write(HighlightedField[] highlightedFields)

    public void write(Parameters qp) throws IOException {
        if (qp == null) {
            return;
        }

        for (Map.Entry<String, Object> kvp : qp.entrySet()) {
            write(kvp.getKey());
            writeParameterValue(kvp.getValue());
        }
    }

    private void writeParameterValue(Object value) throws IOException {
        if (value instanceof String) {
            write((String) value);
        } else if (value instanceof Long) {
            write((Long) value);
        } else if (value instanceof Integer) {
            write((Integer) value);
        } else if (value instanceof Boolean) {
            write((Boolean) value);
        } else if (value == null) {
            // write nothing
        } else if (value instanceof Collection) {
            Iterator iterator = ((Collection) value).iterator();
            while (iterator.hasNext()) {
                writeParameterValue(iterator.next());
            }
        } else {
            write(value.toString());
        }
    }

    public void write(Map<String, String> qp) throws IOException {
        if (qp == null) {
            return;
        }

        for (Map.Entry<String, String> kvp : qp.entrySet()) {
            write(kvp.getKey());
            write(kvp.getValue());
        }
    }
    /* TODO

        public void Write(IReadOnlyList<Facet> facets)
        {
            if (facets == null)
                return;
            for (int i = 0; i < facets.Count; i++)
            {
                var facet = facets[i];
                Write(facet.AggregationField);
                Write(facet.AggregationType);
                Write(facet.DisplayName);
                Write(facet.IncludeRemainingTerms);
                Write(facet.MaxResults);
                Write(facet.Name);
                Write((int)facet.Aggregation);
                Write((int)facet.Mode);
                Write((int)facet.TermSortMode);
                Write(facet.Ranges);
            }
        }
     */

}
