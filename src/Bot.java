import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.TextNode;

import javax.net.ssl.*;
import java.io.IOException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

public class Bot {
    private final String link;

    public Bot(String link) {
        trustEveryone();
        this.link = link;
    }

    public void updateCache() {
        try (Cache cache = Cache.create()){

            List<JobSimple> jobSimples = downloadJobSimples();

            System.out.println("All jobs " + jobSimples.size());

            List<JobSimple> outdated = cache.outdatedJobs(jobSimples);
            System.out.println("Outdated jobs " + outdated.size());

            //outdated
            //List<Job> downloaded = downloadJobs(outdated, cache);
            //System.out.println("Download jobs " + downloaded.size());

            //ALL
            List<Job> downloaded = downloadJobs(jobSimples, cache);
            System.out.println("Download jobs " + downloaded.size());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public List<JobSimple> downloadJobSimples() throws IOException {
        Map<String, Document> pages = allPages(this.link);
        List<JobSimple> simpleJobs = pages.values()
                .stream()
                .flatMap(x -> simpleJobList(x.body()).stream())
                .collect(Collectors.toList());
        return simpleJobs;
    }

    private List<Job> downloadJobs(List<JobSimple> outdated, Cache cache) {
        List<Job> result = new ArrayList<>();

        final ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            List<Future<Optional<Job>>> futures = new ArrayList<>();
            List<Callable<Optional<Job>>> tasks = outdated.stream().map(x -> (Callable<Optional<Job>>) () -> downloadAndProcess(x, cache)).collect(Collectors.toList());
            for (Callable<Optional<Job>> task : tasks) {
                Future<Optional<Job>> future = pool.submit(task);
                futures.add(future);
                Thread.sleep(1000);
            }
            pool.shutdown();
            pool.awaitTermination(5, TimeUnit.MINUTES);
            for (Future<Optional<Job>> future : futures) {
                future.get().ifPresent(result::add);
            }
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace(System.err);
        }
        return result;
    }

    private Optional<Job> downloadAndProcess(JobSimple job, Cache cache) {
        DownlaodData data = downloadJob(job);
        return process(data, cache);
    }

    private Optional<Job> process(DownlaodData data, Cache cache) {
        if (data.error != null) {
            data.error.printStackTrace(System.err);
        }
        else if (data.document != null) {
            System.out.println("Proccessing " + data.document.location());
            Job job = asJob(data.document.body(), data.jobSimple);
            cache.put(job);
            cache.savePage(data.document, job.jobSimple.id);
            System.out.println("Proccessed finished " + data.document.location());
            return Optional.of(job);
        } else {
            System.err.println("Unknown error empty data: " + data.jobSimple.link);
        }
        return Optional.empty();
    }

    private Job asJob(Element body, JobSimple jobSimple) {
        Element descriptionPanel = body.selectFirst("div.panel__desc");
        Element detailsPanel = body.selectFirst("div.ogl__details");
        Element contantPanel = body.selectFirst("div.ogl__contact");
        Optional<Element> details = Optional.ofNullable(detailsPanel)
                .map(x -> x.selectFirst("div#show-address"))
                .map(x -> x.selectFirst("div.ogl__details__desc"));
        String zone = details
                .map(this::extractZone)
                .orElse("");
        String street2 = details.map(x -> x.textNodes().stream().skip(1).findFirst().map(TextNode::text).orElse("")).orElse("");

        String street = Optional.ofNullable(contantPanel)
                .map(x -> x.selectFirst("div#show-kontakt_ulica"))
                .map(x -> x.selectFirst("div.ogl__details__desc"))
                .map(x -> x.selectFirst("span"))
                .map(Element::text)
                .orElse("");

        return new Job(jobSimple, StreetParser.parse(street), StreetParser.parse(street2), zone);
    }

    private String extractZone(Element zone) {
        Optional<String> prefix = Optional.empty();
        List<TextNode> textNodes = zone.textNodes();
        if (!textNodes.isEmpty()) {
            prefix = Optional.ofNullable(textNodes.get(0).text());
        }
        Optional<String> suffix = Optional.of(zone).map(x -> x.selectFirst("a")).map(Element::text);
        return prefix.orElse("") + suffix.orElse("");
    }

    private DownlaodData downloadJob(JobSimple job) {
        Document document = null;
        Exception error = null;
        try {
            System.out.println("downloading: " + job.link);
            document = Jsoup.connect(job.link).get();
            System.out.println("downloaded: " + job.link);
        } catch (IOException e) {
            error = e;
        }
        return new DownlaodData(document, job, error);
    }

    private static class DownlaodData {
        final Document document;
        final JobSimple jobSimple;
        final Exception error;

        DownlaodData(Document document, JobSimple job, Exception error) {
            this.document = document;
            this.jobSimple = job;
            this.error = error;
        }
    }

    private Map<String, Document> allPages(String root) throws IOException {
        Map<String, Document> result = new HashMap<>();

        LinkedList<String> queue = new LinkedList<>();
        queue.push(root);

        while(!queue.isEmpty()) {
            String link = queue.pop();

            Document doc = Jsoup.connect(link).get();
            System.out.println("Page downlaoded: " + link);
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

        Optional<Element> companyPanel = Optional.of(listItem).map(x -> x.selectFirst("p.list__item__details__info"));
        String company = companyPanel.map(x -> x.selectFirst("a"))
                .map(Element::text)
                .orElse(companyPanel.map(Element::text).orElse(""));
        return new JobSimple(id, link, title, dateString, company);
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
