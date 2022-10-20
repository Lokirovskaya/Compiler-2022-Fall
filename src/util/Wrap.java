package util;

public class Wrap<T> {
    private T data;

    public Wrap(T init) {
        this.data = init;
    }

    public T get() {
        return data;
    }

    public void set(T data) {
        this.data = data;
    }
}
