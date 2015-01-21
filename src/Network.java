import java.util.ArrayList;
import java.util.List;

/**
 * Created by Cian on 20/01/2015.
 */
public class Network {

    private List<Router> routers;
    private int cost;
    private int bandwidth;
    private int traffic;

    public Network(){
        routers = new ArrayList<>();
    }

    public void addRouter(Router router){
        routers.add(router);
    }

    public void setTraffic(int traffic){
        this.traffic = traffic;
    }

    public int getBandwidth(){
        return bandwidth;
    }

    public int getCost(){
        return cost;
    }

    public List<Router> getRouters(){
        return routers;
    }

}
