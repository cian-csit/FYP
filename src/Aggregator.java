import java.util.*;

/**
 * Created by Cian on 20/01/2015.
 */
public class Aggregator extends Thread{
    private Map<Network, List<Integer>> trafficValues;

    public Aggregator(){
        trafficValues = new HashMap<>();
    }

    public synchronized void update(int traffic, Network network){
        List<Integer> values = trafficValues.get(network);
        if (values == null){
            values = new ArrayList<>();
        }
        values.add(traffic);
    }

    public void run(){

        for (Map.Entry<Network, List<Integer>> entry : trafficValues.entrySet()){
            Network network = entry.getKey();
            List<Integer> values = entry.getValue();
            int max = 0;
            if (!values.isEmpty()){
                max = Collections.max(values);
            }

            network.setTraffic(max);

            // Clear list
            trafficValues.put(network, new ArrayList<>());

            if (max < 10000){
                List<Router> routers = network.getRouters();

            }
        }

        try{
            Thread.sleep(1000);
        } catch(InterruptedException e){
            e.printStackTrace();
        }
    }
}
