import me.legrange.mikrotik.ApiConnection;
import me.legrange.mikrotik.MikrotikApiException;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Cian on 19/01/2015.
 */
public class Main {

    private static Map<String, Network> networks;

    public static void main(String[] args) throws UnknownHostException, MikrotikApiException, InterruptedException{

        networks = new HashMap<>();
        Router dublin = new Router(InetAddress.getByName("10.10.100.2"));
        dublin.execute("/ip/address/print");
        //dublin.begin();

    }

    public static Map<String, Network> getNetworks(){
        return networks;
    }

    public static void addNetwork(String addr, Network network){
        networks.put(addr, network);
    }
}
