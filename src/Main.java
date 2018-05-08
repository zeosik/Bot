import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.TextNode;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Main {
    public static void main(String[] args) {
        // Prints "Hello, World" to the terminal window.
        System.out.println("Hello, World");

        String link = "https://ogloszenia.trojmiasto.pl/praca-zatrudnie/slb,4,slwp,57.html";
        Bot bot = new Bot(link);
        bot.updateCache();

        Cache cache = Cache.create();

        //updateSimpleCache(cache, bot);

        Map<String, List<Job>> byZone = cache.jobs().stream().collect(Collectors.groupingBy(x -> x.zone));
        for (Map.Entry<String, List<Job>> zoneEntry : byZone.entrySet().stream().sorted(Comparator.comparing(x -> x.getValue().size())).collect(Collectors.toList())) {
            System.out.println(MessageFormat.format("{1}: {0}", zoneEntry.getKey(), zoneEntry.getValue().size()));
            Map<String, List<Job>> byStreet = zoneEntry.getValue().stream().collect(Collectors.groupingBy(x -> x.street.name + x.street2.name));
            for (Map.Entry<String, List<Job>> streetEntry : byStreet.entrySet().stream().sorted(Comparator.comparing(x -> x.getValue().size())).collect(Collectors.toList())) {
                System.out.println(MessageFormat.format("   {1}: {0}", streetEntry.getKey(), streetEntry.getValue().size()));
                for (String company : streetEntry.getValue().stream().map(x -> x.jobSimple.company).distinct().collect(Collectors.toList())) {
                    System.out.println(MessageFormat.format("       {0}", company));
                }
            }
        }
        cache.close();
    }

    public static void updateSimpleCache(Cache cache, Bot bot) {
        try {
            List<JobSimple> jobSimples = bot.downloadJobSimples();
            for (JobSimple jobSimple : jobSimples) {
                Job job = cache.cached.get(jobSimple.id);
                cache.put(new Job(jobSimple, job.street, job.street2, job.zone));
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

}
