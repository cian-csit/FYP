import me.legrange.mikrotik.ApiConnection;
import me.legrange.mikrotik.MikrotikApiException;
import me.legrange.mikrotik.ResultListener;

import java.util.Map;

public class RouterInterface {

    private String address;

    private Router parentRouter;
    private int traffic;
    private String label;
    private String tag;
    private Aggregator aggregator;
    private int linkSpeed;

    public RouterInterface(String address, String label, Router parentRouter, int linkSpeed){
        this.address = address;
        this.label = label;
        this.parentRouter = parentRouter;
        this.linkSpeed = linkSpeed;
        this.aggregator = new Aggregator(parentRouter, linkSpeed, label);
    }


    public void begin(ApiConnection conn) throws MikrotikApiException {
        aggregator.periodicAverage();
        tag = conn.execute("/interface/monitor-traffic interface=" + label,
                new ResultListener() {

                    public void receive(Map<String, String> result) {
                        //System.out.println(result);
                        int in = Integer.parseInt(result.get("rx-bits-per-second"));
                        int out = Integer.parseInt(result.get("tx-bits-per-second"));
                        traffic = in + out;
                        System.out.println("Total traffic on " + label +": " + (traffic));
                        aggregator.update(traffic);
                    }

                    public void error(MikrotikApiException e) {
                        System.out.println("An error occurred: " + e.getMessage());
                    }

                    public void completed() {
                        System.out.println("Asynchronous command has finished");
                    }
                }
        );
        // Let it run for 60 seconds
        //Thread.sleep(60000);
        //conn.cancel(tag);
    }
}
