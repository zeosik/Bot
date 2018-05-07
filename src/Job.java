public class Job {

    public final String street;
    public final JobSimple jobSimple;

    public Job(JobSimple jobSimple, String street) {
        this.jobSimple = jobSimple;
        this.street = street;
    }
}
