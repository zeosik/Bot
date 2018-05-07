import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class Bot {
    private final String link;

    public Bot(String link) {
        this.link = link;
    }

    public void updateCache() {
        try {
            Document doc = Jsoup.connect(this.link).get();
            Element list = doc.selectFirst("div.ogl__list__wrap");
            list.select("div.list__item")
                    .stream().map(this::asJobSimple)
                    .forEach(System.out::println);

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private JobSimple asJobSimple(Element listItem) {
        String id = listItem.attr("id");
        Element a = listItem.selectFirst("a.list__item__content__title__name");
        String link = a.attr("href");
        String title = a.text();
        String dateString = listItem.selectFirst("div.list__item__footer").text();
        return new JobSimple(id, link, title, dateString);
    }
}
