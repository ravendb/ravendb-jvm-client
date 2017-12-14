package net.ravendb.client.primitives;

public class Tuple<A,B> {
    public final A first;
    public final B second;

    private Tuple(A first, B second) {
        this.first = first;
        this.second = second;
    }

    public static <A, B> Tuple<A, B> create(A first, B second) {
        return new Tuple<>(first, second);
    }
}
