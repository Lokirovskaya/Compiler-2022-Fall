package util;

import java.util.Objects;

public class UnorderedPair<T> extends Pair<T, T> {
    public UnorderedPair(T first, T second) {
        super(first, second);
        if (first.hashCode() > second.hashCode()) {
            T t = super.first;
            super.first = super.second;
            super.second = t;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Pair<?, ?> pair = (Pair<?, ?>) o;
        return Objects.equals(first, pair.first) && Objects.equals(second, pair.second)
                || Objects.equals(first, pair.second) && Objects.equals(second, pair.first);
    }

    @Override
    public int hashCode() {
        return Objects.hash(first, second);
    }
}
