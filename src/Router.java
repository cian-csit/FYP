import com.tufar.IPCalculator.IPv4;
import me.legrange.mikrotik.ApiConnection;
import me.legrange.mikrotik.MikrotikApiException;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


/**
 * Class which represents an individual router and it's associated interfaces and functions
 * @author Cian O'Halloran
 */
public class Router {

    private InetAddress addr;
    private ApiConnection conn;
    private List<RouterInterface> interfaces;
    private String routerID;
    private String username;
    private String password;

    /**
     * Constructs a new Router object
     * @param addr The IP address used to connect to this router
     * @param username Username used to log into the router
     * @param password Password used to log into the router
     */
    public Router(InetAddress addr, String username, String password){
        this.addr = addr;
        this.username = username;
        this.password = password;
    }

    /**
     * Detects all interfaces on the current router which have an IP address assigned to them.
     * @throws MikrotikApiException
     * @throws InterruptedException
     */
    public void detectInterfaces() throws MikrotikApiException, InterruptedException{

        List<Map<String, String>> routerIDquery = conn.execute("/routing/ospf/instance/print");
        routerID = routerIDquery.get(0).get("router-id");
        interfaces = new ArrayList<>();

        List<Map<String, String>> result = conn.execute("/ip/address/print");
        System.out.println(result);
        for (Map<String, String> r : result){
            createInterface(r);
        }

        Thread.sleep(60000);
    }

    /**
     * Parses interface information and creates a corresponding {@link RouterInterface} object, then starts the
     * interface's listening function.
     * @param interfaceResult Map data for a single router interface
     * @throws MikrotikApiException Thrown if
     */
    private void createInterface(Map<String, String> interfaceResult)
            throws MikrotikApiException {

        String interfaceAddress = interfaceResult.get("address");
        String interfaceLabel = interfaceResult.get("interface");

        int bandwidth = getBandwidth(interfaceAddress);
        System.out.println("Adding interface object for " + interfaceAddress + " on interface " + interfaceLabel );
        RouterInterface netInterface = new RouterInterface(interfaceAddress, interfaceLabel, this, bandwidth);
        netInterface.begin(conn);
        interfaces.add(netInterface);
    }

    private int getBandwidth(String address) {
        Map<String, Integer> linkBandwidth = Main.getLinkBandwidth();
        for (Map.Entry<String, Integer> entry : linkBandwidth.entrySet()) {
            IPv4 link = new IPv4(entry.getKey());
            if (link.contains(address)) {
                return entry.getValue();
            }
        }

        // No matching bandwidth value found. Throw exception.
        throw new IllegalArgumentException("No valid network for " + address + "found in config file!");
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
        conn.login(username, password);
    }

}
