package intercode;

public class Label {
    public final String name;

    public Label(String s) {
        this.name = s;
    }

    @Override
    public String toString() {
        return name;
    }
}
