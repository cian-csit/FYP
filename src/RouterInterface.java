import me.legrange.mikrotik.ApiConnection;
import me.legrange.mikrotik.MikrotikApiException;
import me.legrange.mikrotik.ResultListener;

import java.util.Map;

/**
 * Class which represents a single network interface on the router
 * @author Cian O'Halloran
 */
public class RouterInterface {

    private int traffic;
    private final String label;
    private final Aggregator aggregator;
    private final String parentIdentity;
    private final Router parentRouter;
    private final int bandwidth;

    /**
     * Constructor to create new object
     * @param label The label of the network interface (ether1, ether2, etc)
     * @param parentRouter The {@link Router} which contains the interface
     * @param parentIdentity The identity of the parent {@link Router}
     * @param bandwidth The maximum speed (in Mbps) of the interface
     */
    public RouterInterface(String label, Router parentRouter, String parentIdentity, int bandwidth){
        this.label = label;
        this.parentRouter = parentRouter;
        this.parentIdentity = parentIdentity;
        this.bandwidth = bandwidth;
        this.aggregator = new Aggregator(parentRouter, bandwidth, label);
    }

    /**
     * Sets initial OSPF cost depending on available bandwidth
     */
    public void setInitialCost(){
        int cost = 100 / bandwidth;
        try {
            parentRouter.execute("/routing/ospf/interface/add interface=" + label + " network-type=broadcast cost=" + cost);
        }catch(MikrotikApiException e){
            System.out.println("Interface has already been set manually. Skipping...");
        }
    }

    /**
     * Listens on the network interface and pulls traffic data as it comes available
     * @param conn The API connection used by the router
     * @throws MikrotikApiException If an invalid command is sent to the router
     */
    public void listen(ApiConnection conn) throws MikrotikApiException {
        aggregator.periodicAverage();
        conn.execute("/interface/monitor-traffic interface=" + label,
                new ResultListener() {

                    public void receive(Map<String, String> result) {
                        int in = Integer.parseInt(result.get("rx-bits-per-second"));
                        int out = Integer.parseInt(result.get("tx-bits-per-second"));
                        traffic = in + out;
                        System.out.println(Thread.currentThread().getId() + parentIdentity + ": Total traffic on " + label +": " + (traffic));
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
    }
}
