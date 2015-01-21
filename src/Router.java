import me.legrange.mikrotik.ApiConnection;
import me.legrange.mikrotik.MikrotikApiException;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by Cian on 19/01/2015.
 */
public class Router {

    private InetAddress addr;
    private ApiConnection conn;
    private List<RouterInterface> interfaces;
    private String routerID;

    public Router(InetAddress addr) throws MikrotikApiException, InterruptedException{
        this.addr = addr;
        connect();
        detectInterfaces();
        List<Map<String, String>> result = conn.execute("/routing/ospf/instance/print");
        routerID = result.get(0).get("router-id");
    }

    public void detectInterfaces() throws MikrotikApiException, InterruptedException{

        Map<String, Network> networks = Main.getNetworks();




        List<Map<String, String>> result = conn.execute("/ip/address/print");
        System.out.println(result);
        interfaces = new ArrayList<>();
        for (Map<String, String> r : result){
            String address = r.get("address");
            String networkStr = r.get("network");
            Network networkObj = networks.get(networkStr);
            if (networkObj == null){
                networkObj = new Network();
                Main.addNetwork(networkStr, networkObj);
            }
            networkObj.addRouter(this);

            String interfaceLabel = r.get("interface");
            System.out.println("Adding interface object for " + address + " on interface " + interfaceLabel );
            RouterInterface netInterface = new RouterInterface(address, interfaceLabel, networkObj);
            netInterface.begin(conn);
            interfaces.add(netInterface);
        }
        Thread.sleep(60000);
    }

    public void execute(String cmd) throws MikrotikApiException, InterruptedException{
        // Ensure connection is established before issuing command
        if (conn == null || !conn.isConnected()){
            connect();
        }
        System.out.println("Executing " + cmd);
        List<Map<String, String>> result = conn.execute(cmd);
        System.out.println(result);
    }

    public void connect() throws MikrotikApiException, InterruptedException{
        conn = ApiConnection.connect(addr.getHostAddress());
        conn.login("admin", "");
    }

}
