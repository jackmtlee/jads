package jads.mp.util;

public class Pair<T1, T2> {

    public final T1 first;
    public final T2 second;

    public Pair(T1 first, T2 second) {
        this.first = first;
        this.second = second;
    }

    public int compareTo(Pair<? extends Comparable<T1>, ? extends Comparable<T2>> pair) {
        int result = pair.first.compareTo(first);
        if (result != 0) return result;
        return pair.second.compareTo(second);
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Pair<?, ?> pair = ( Pair<?, ?> ) o;

        if (first != null ? !first.equals(pair.first) : pair.first != null)
            return false;
        return second != null ? second.equals(pair.second) : pair.second == null;
    }

    public int hashCode() {
        int result = first != null ? first.hashCode() : 0;
        result = 31 * result + (second != null ? second.hashCode() : 0);
        return result;
    }
}
