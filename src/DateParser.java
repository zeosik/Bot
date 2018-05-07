import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DateParser {

    private static Pattern DATE_PATTERN = Pattern.compile("Aktualizacja: (\\d+) (.*) temu.*");

    private static Parser YEAR_PARSER = new Parser(Calendar.YEAR, "rok", "lat");
    private static Parser MONTH_PARSER = new Parser(Calendar.MONTH, "miesi");
    private static Parser DAY_PARSER = new Parser(Calendar.DAY_OF_YEAR, "dzień", "dni");
    private static Parser HOUR_PARSER = new Parser(Calendar.HOUR_OF_DAY, "godz");
    private static Parser MINUTE_PARSER = new Parser(Calendar.MINUTE, "minu");

    private static List<Parser> PARSERS = Arrays.asList(YEAR_PARSER, MONTH_PARSER, DAY_PARSER, HOUR_PARSER, MINUTE_PARSER);

    public static Date parseDate(String dateString) {
        Matcher matcher = DATE_PATTERN.matcher(dateString);
        if (!matcher.matches()) {
            throw new RuntimeException("Could not parse dateString: " + dateString);
        }
        int number = Integer.valueOf(matcher.group(1));
        String type = matcher.group(2);
        Optional<Parser> parser = PARSERS.stream().filter(x -> x.accepts(type)).findAny();
        if (!parser.isPresent()) {
            throw new RuntimeException("Could not parse dateString, unknown date type: " + type);
        }
        return parser.get().parse(number);
    }

    public static class Parser {

        private int calendarField;
        private List<String> prefix;

        public Parser(int calendarField, String... prefix) {
            this.calendarField = calendarField;
            this.prefix = Arrays.asList(prefix);
        }

        public boolean accepts(String type) {
            return this.prefix.stream().anyMatch(type::startsWith);
        }

        public Date parse(int number) {
            Calendar cal = Calendar.getInstance();
            cal.add(this.calendarField, -number);
            ///..
            PARSERS.stream().skip(PARSERS.indexOf(this) + 1).forEach(x -> cal.set(x.calendarField, 0));
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
            ///...
            return cal.getTime();
        }
    }
}
