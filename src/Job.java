public class Job {

    public final JobSimple jobSimple;
    public final Street street;
    public final Street street2;
    public final String zone;

    public Job(JobSimple jobSimple, Street street, Street street2, String zone) {
        this.jobSimple = jobSimple;
        this.street = street;
        this.street2 = street2;
        this.zone = zone;
    }
}
