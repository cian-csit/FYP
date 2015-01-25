import me.legrange.mikrotik.MikrotikApiException;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Class which aggregates all traffic data from a particular connection, and periodically adjusts the OSPF cost
 * based on the traffic.
 * @author Cian O'Halloran
 */
public class Aggregator {

    private List<Integer> traffic;
    private final Router parentRouter;
    private int avgTraffic;
    private final ScheduledExecutorService scheduler;
    private final int linkSpeed;
    private final String interfaceLabel;

    /**
     * Constructs a newly allocated Aggregator object for the current interface
     * @param parentRouter The Router object
     * @param linkSpeed The maximum speed of the link attached to that interface
     * @param interfaceLabel The label of the interface (ether1, ether2, etc.)
     */
    public Aggregator(Router parentRouter, int linkSpeed, String interfaceLabel){
        traffic = new ArrayList<>();
        this.parentRouter = parentRouter;
        this.linkSpeed = linkSpeed;
        this.interfaceLabel = interfaceLabel;
        scheduler = Executors.newScheduledThreadPool(1);
    }

    /**
     * Adds a traffic value to the List
     * @param trafficValue The new value to be added
     */
    public void update(int trafficValue){
        //System.out.println("Adding value: " + trafficValue + " to aggregator");
        traffic.add(trafficValue);
    }

    /**
     * Calculates the average of the contents of {@code traffic} every two minutes
     */
    public void periodicAverage(){
        System.out.println("Starting periodic average on " + interfaceLabel);
        final Runnable averageTask = new Runnable() {
            public void run() {
                int sum = 0, avg = 0;
                if (!traffic.isEmpty()) {
                    for (int value : traffic) {
                        sum += value;
                    }
                    // Don't need double precision, so don't cast to double.
                    avg = sum/traffic.size();
                }
                avgTraffic = avg;
                traffic = new ArrayList<>();

                // Get new cost
                try {
                    getNewCost(avgTraffic);
                }catch (MikrotikApiException | InterruptedException e){
                    e.printStackTrace();
                }
            }
        };
        scheduler.scheduleAtFixedRate(averageTask, 10, 10, TimeUnit.SECONDS);
    }

    private void getNewCost(int traffic) throws MikrotikApiException, InterruptedException{
        // Get current cost
        List<Map<String, String>> result = parentRouter.execute("/routing/ospf/interface/print where interface=" + interfaceLabel);
        int currentCost = Integer.parseInt(result.get(0).get("cost"));

        System.out.println("Current cost is: " + currentCost);

        int newCost;

        if ((traffic / (double)linkSpeed) > 0.8 ){
            newCost = currentCost + 10;
            System.out.println(interfaceLabel + ": Incrementing cost");
            setNewCost(newCost);
        }
        else if (((traffic / (double)linkSpeed) < 0.4) && (currentCost > 10)){
            newCost = currentCost - 10;
            System.out.println(interfaceLabel + ": Decrementing cost");
            setNewCost(newCost);
        }
        else{
            System.out.println(interfaceLabel + ": Making no change");
        }
    }

    private void setNewCost(int cost) throws MikrotikApiException, InterruptedException{
        parentRouter.execute("/routing/ospf/interface/add interface=" + interfaceLabel + " network-type=broadcast cost=" + cost);
    }
}
