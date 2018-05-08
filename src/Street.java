import java.text.MessageFormat;

public class Street {
    public final String name;
    public final String number;

    public Street(String name, String number) {
        this.name = name;
        this.number = number;
    }

    public String toString() {
        return MessageFormat.format("Street[{0} - {1}]", name, number);
    }
}
