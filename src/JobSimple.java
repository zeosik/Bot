import java.text.MessageFormat;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JobSimple {
    public final String id;
    public final String link;
    public final String title;
    public final String dateString;
    public final Date date;

    private static Pattern DATE_PATTERN = Pattern.compile("Aktualizacja: (\\d+) (dni|dzień|godzin|godziny) temu.*");
    //private static Map<String, Function<String, Date>>

    public JobSimple(String id, String link, String title, String dateString) {
        this.id = id;
        this.link = link;
        this.title = title;
        this.dateString = dateString;
        this.date = parseDate(dateString);
    }

    private Date parseDate(String dateString) {
        Matcher matcher = DATE_PATTERN.matcher(dateString);
        if (!matcher.matches()) {
            throw new RuntimeException("Could not parse dateString: " + dateString);
        }
        String number = matcher.group(1);
        String type = matcher.group(2);
        //TODO
        return new Date();
    }

    @Override
    public String toString() {
        return MessageFormat.format("JobSimple[{0}--{2}]", id, link, title, date);
    }
}
