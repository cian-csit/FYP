import me.legrange.mikrotik.MikrotikApiException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
    private final String parentRouterId;
    private int avgTraffic;
    private final ScheduledExecutorService scheduler;
    private final int linkSpeed;
    private final String interfaceLabel;
    private int minutesSinceLastChange;
    private final int checkPeriod = 2; // How often to check average traffic
    private final int threshold = 5;   // Period to wait between cost changes

    /**
     * Constructs a newly allocated Aggregator object for the current interface
     * @param parentRouter The Router object
     * @param linkSpeed The maximum speed (in Mbps) of the link attached to that interface
     * @param interfaceLabel The label of the interface (ether1, ether2, etc.)
     */
    public Aggregator(Router parentRouter, int linkSpeed, String interfaceLabel){
        traffic = new ArrayList<>();
        this.parentRouter = parentRouter;
        this.linkSpeed = linkSpeed;
        this.interfaceLabel = interfaceLabel;
        parentRouterId = parentRouter.getIdentity();
        scheduler = Executors.newScheduledThreadPool(1);
        minutesSinceLastChange = 0;
    }

    /**
     * Adds a traffic value to the List
     * @param trafficValue The new value to be added
     */
    public void update(int trafficValue){
        traffic.add(trafficValue);
    }

    /**
     * Calculates the average of the contents of {@code traffic} every two minutes
     */
    public void periodicAverage(){
        System.out.println("Starting periodic average on " + interfaceLabel);
        final Runnable averageTask = () -> {
            minutesSinceLastChange += checkPeriod;
            int sum = 0, avg = 0;
            // Calculate average traffic
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
                List<Map<String, String>> currentCostRaw = fetchCurrentCost();
                int currentCost = Integer.parseInt(currentCostRaw.get(0).get("cost"));
                String id = currentCostRaw.get(0).get(".id");

                int newCost = calculateNewCost(avgTraffic, currentCost);

                // Change cost if it hasn't been changed recently
                if (newCost != 0 && minutesSinceLastChange >= threshold) {
                    minutesSinceLastChange = 0;
                    setNewCost(newCost, id);
                }
            }catch (MikrotikApiException e){
                e.printStackTrace();
            }
        };
        scheduler.scheduleAtFixedRate(averageTask, checkPeriod, checkPeriod, TimeUnit.MINUTES);
    }

    /**
     * Calculates the new cost of the interface based on the traffic
     * @param rawTraffic The traffic value in bits per second
     * @param currentCost The current OSPF cost on the interface
     * @throws MikrotikApiException If an invalid command is sent to the router
     */
    private int calculateNewCost(int rawTraffic, int currentCost) throws MikrotikApiException {
        double traffic = rawTraffic / 1000000.0; // Convert bits to Megabits (Mb)
        double usedBandwidth = (traffic / linkSpeed) * 100 ; // Percentage of bandwidth used
        usedBandwidth = Math.round(usedBandwidth * 100) / 100; // Round to two decimal places

        System.out.print(parentRouterId + ":" + interfaceLabel + ": Traffic at " + traffic + "Mbps (" + usedBandwidth + "%) - ");

        if (usedBandwidth > 80 ){
            System.out.println("Incrementing cost from " + currentCost + " to " + (currentCost + 10));
            return currentCost + 10;
        }
        else if (usedBandwidth < 30 && currentCost > 10){
            System.out.println("Decrementing cost from " + currentCost + " to " + (currentCost - 10));
            return currentCost - 10;
        }
        else if (usedBandwidth < 30 && currentCost < 10){
            System.out.println("Cost already at minimum. Making no change");
            return 0;
        }
        else{
            System.out.println("Making no change");
            return 0;
        }
    }

    /**
     * Fetches the OSPF data for the current interface
     * @return The raw un-formatted OSPF data
     * @throws MikrotikApiException If an invalid command is sent to the router
     */
    private List<Map<String, String>> fetchCurrentCost() throws MikrotikApiException{
        return parentRouter.execute("/routing/ospf/interface/print where interface=" + interfaceLabel);
    }

    /**
     * Sets the new OSPF cost for the current interface
     * @param cost The new OSPF cost
     * @param id The id corresponding to the current interface
     * @throws MikrotikApiException If an invalid command is sent to the router
     */
    private void setNewCost(int cost, String id) throws MikrotikApiException {
        try {
            parentRouter.execute("/routing/ospf/interface/set .id=" + id + " cost=" + cost);
        }catch(MikrotikApiException e){
            System.out.println("Interface is set as dynamic. Adding non-dynamic interface... ");
            parentRouter.execute("/routing/ospf/interface/add interface=" + interfaceLabel + " network-type=broadcast cost=" + cost);
        }
    }
}
