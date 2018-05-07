import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import javax.net.ssl.*;
import java.io.IOException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.stream.Collectors;

public class Bot {
    private final String link;

    public Bot(String link) {
        this.link = link;
    }

    public void updateCache() {
        try (Cache cache = new Cache("cache.xml", "cache");){
            trustEveryone();

            Map<String, Document> pages = allPages(this.link);
            List<JobSimple> jobs = pages.values()
                    .stream()
                    .flatMap(x -> simpleJobList(x.body()).stream())
                    .collect(Collectors.toList());

            System.out.println("All jobs " + jobs.size());

            List<JobSimple> outdated = cache.outdatedJobs(jobs);
            System.out.println("Outdated jobs " + outdated.size());

            //todo
            outdated.stream().map(x -> new Job(x, "tmp")).forEach(cache::put);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Map<String, Document> allPages(String root) throws IOException {
        Map<String, Document> result = new HashMap<>();

        LinkedList<String> queue = new LinkedList<>();
        queue.push(root);

        while(!queue.isEmpty()) {
            String link = queue.pop();

            Document doc = Jsoup.connect(link).get();
            result.put(link, doc);

            Optional<String> next = nextPage(doc.body());
            next.ifPresent(queue::push);
        }

        return result;
    }

    private Optional<String> nextPage(Element body) {
        return Optional.ofNullable(body)
                .map(x -> x.selectFirst("li.pagination__page__next"))
                .map(x -> x.selectFirst("a.pagination__page__nextlink"))
                .map(x -> x.attr("abs:href"));
    }

    private List<JobSimple> simpleJobList(Element body) {
        Element list = body.selectFirst("div.ogl__list__wrap");
        return list.select("div.list__item")
                .stream()
                .map(this::asJobSimple)
                .collect(Collectors.toList());
    }

    private JobSimple asJobSimple(Element listItem) {
        String id = listItem.attr("id");
        Element a = listItem.selectFirst("a.list__item__content__title__name");
        String link = a.attr("abs:href");
        String title = a.text();
        String dateString = listItem.selectFirst("div.list__item__footer").text();
        return new JobSimple(id, link, title, dateString);
    }

    private void trustEveryone() {
        try {
            HttpsURLConnection.setDefaultHostnameVerifier(new HostnameVerifier(){
                public boolean verify(String hostname, SSLSession session) {
                    return true;
                }});
            SSLContext context = SSLContext.getInstance("TLS");
            context.init(null, new X509TrustManager[]{new X509TrustManager(){
                public void checkClientTrusted(X509Certificate[] chain,
                                               String authType) throws CertificateException {}
                public void checkServerTrusted(X509Certificate[] chain,
                                               String authType) throws CertificateException {}
                public X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[0];
                }}}, new SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(
                    context.getSocketFactory());
        } catch (Exception e) { // should never happen
            e.printStackTrace();
        }
    }
}
