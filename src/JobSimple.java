import java.text.DateFormat;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JobSimple {
    public final String id;
    public final String link;
    public final String title;
    public final Date date;
    final String company;


    //private static Map<String, Function<String, Date>>

    public JobSimple(String id, String link, String title, String dateString, String company) {
        this.id = id;
        this.link = link;
        this.title = title;
        this.date = DateParser.parseDate(dateString);
        this.company = company;
    }


    @Override
    public String toString() {
        DateFormat df = DateFormat.getDateInstance(DateFormat.MEDIUM);
        return MessageFormat.format("JobSimple[{0}--{2}--{3}]", id, link, title, df.format(date));
    }
}
