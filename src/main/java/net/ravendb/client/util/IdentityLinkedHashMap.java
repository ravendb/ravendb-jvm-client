package net.ravendb.client.util;

import com.google.common.base.Equivalence;
import com.google.common.collect.Iterators;

import java.util.*;

public class IdentityLinkedHashMap<K, T>  extends AbstractMap<K,T> {

    private final static Equivalence<Object> equivalence = Equivalence.identity();

    private IdentityLinkedHashSet set = new IdentityLinkedHashSet();

    @Override
    public Set<Entry<K, T>> entrySet() {
        return set;
    }

    @Override
    public T put(K k, T t) {
        return set.innerMap.put( equivalence.wrap(k), t);
    }

    @Override
    public boolean containsKey(Object key) {
        return set.contains(key);
    }

    @Override
    public T remove(Object key) {
        return set.innerMap.remove(equivalence.wrap(key));
    }

    @Override
    public T get(Object key) {
        return set.innerMap.get(equivalence.wrap(key));
    }

    public class MyEntry implements Entry<K, T> {

        final Entry<Equivalence.Wrapper<K>, T> entry;

        public MyEntry(Entry<Equivalence.Wrapper<K>, T> entry) {
            this.entry = entry;
        }

        @Override
        public K getKey() {
            return entry.getKey().get();
        }

        @Override
        public T getValue() {
            return entry.getValue();
        }

        @Override
        public T setValue(T value) {
            return entry.setValue(value);
        }
    }

    public class IdentityLinkedHashSet extends AbstractSet<Entry<K,T>> {

        Map<Equivalence.Wrapper<K>, T> innerMap = new LinkedHashMap<>();

        @Override
        public Iterator<Entry<K, T>> iterator() {
            return Iterators.transform(innerMap.entrySet().iterator(), entry -> new MyEntry(entry));
        }

        @Override
        public boolean add(Entry<K, T> entry) {
            Equivalence.Wrapper<K> wrap = equivalence.wrap(entry.getKey());
            innerMap.put(wrap, entry.getValue());
            return true;
        }

        @Override
        public int size() {
            return innerMap.size();
        }

        @Override
        public boolean contains(Object key) {
            return innerMap.containsKey(equivalence.wrap(key));
        }
    }
}
