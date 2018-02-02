package jads.mp.util;

public class PairInt<T> implements Comparable<PairInt<T>> {

    public final int first;
    public final T second;

    public PairInt(int first, T second) {
        this.first = first;
        this.second = second;
    }

    public int compareTo(PairInt<T> pair) {
        return Integer.compare(first, pair.first);
    }
}
