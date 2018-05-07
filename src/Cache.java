import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.StaxDriver;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Cache implements Closeable {

    private final String cacheFilePath;
    HashMap<String, Job> cached;

    public Cache(String cacheFilePath, String cacheDir) {
        this.cacheFilePath = cacheFilePath;
        XStream xStream = new XStream(new StaxDriver());
        File cacheFile = new File(cacheFilePath);
        if (cacheFile.exists()) {
            this.cached = (HashMap<String, Job>) xStream.fromXML(cacheFile);
        } else {
            this.cached = new HashMap<>();
        }
    }

    public List<JobSimple> outdatedJobs(List<JobSimple> all) {
        return all.stream().filter(this::isOutdated).collect(Collectors.toList());
    }

    private boolean isOutdated(JobSimple job) {
        return !this.cached.containsKey(job.id) || this.cached.get(job.id).jobSimple.date.before(job.date);
    }

    public void put(Job job) {
        this.cached.put(job.jobSimple.id, job);
    }

    @Override
    public void close() {
        XStream xStream = new XStream(new StaxDriver());
        String xml = xStream.toXML(this.cached);
        try {
            Files.write(Paths.get(this.cacheFilePath), xml.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
