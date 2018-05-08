import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class StreetParser {

    static Street parse(String street) {
        String[] parts = street.split(" ");
        boolean numberStart = false;
        List<String> nameParts = new ArrayList<>();
        List<String> numberParts = new ArrayList<>();
        for (String part : parts) {
            if (!part.isEmpty() && Character.isDigit(part.charAt(0))) {
                numberStart = true;
            }
            (numberStart ? numberParts : nameParts).add(part);
        }
        String name = nameParts.stream().collect(Collectors.joining(" "));
        String number = numberParts.stream().collect(Collectors.joining(" "));
        return new Street(name, number);
    }
}
