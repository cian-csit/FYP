import me.legrange.mikrotik.ApiConnection;
import me.legrange.mikrotik.MikrotikApiException;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Router {

    private InetAddress addr;
    private ApiConnection conn;
    private List<RouterInterface> interfaces;
    private String routerID;


    public Router(InetAddress addr){
        this.addr = addr;
    }

    public void detectInterfaces() throws MikrotikApiException, InterruptedException{

        List<Map<String, String>> routerIDquery = conn.execute("/routing/ospf/instance/print");
        routerID = routerIDquery.get(0).get("router-id");

        List<Map<String, String>> result = conn.execute("/ip/address/print");
        System.out.println(result);
        interfaces = new ArrayList<>();
        for (Map<String, String> r : result){
            String address = r.get("address");
            String interfaceLabel = r.get("interface");

            System.out.println("Adding interface object for " + address + " on interface " + interfaceLabel );
            RouterInterface netInterface = new RouterInterface(address, interfaceLabel, this);
            netInterface.begin(conn);
            interfaces.add(netInterface);
        }
        Thread.sleep(60000);
    }

    public synchronized List<Map<String, String>> execute(String cmd) throws MikrotikApiException, InterruptedException{
        // Ensure connection is established before issuing command
        if (conn == null || !conn.isConnected()){
            connect();
        }
        System.out.println("Executing " + cmd);
        List<Map<String, String>> result = conn.execute(cmd);
        System.out.println(result);
        return result;
    }

    public void connect() throws MikrotikApiException, InterruptedException{
        conn = ApiConnection.connect(addr.getHostAddress());
        conn.login("admin", "");
    }

}
