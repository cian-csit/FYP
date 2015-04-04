import me.legrange.mikrotik.ApiConnection;
import me.legrange.mikrotik.MikrotikApiException;
import org.apache.commons.net.util.SubnetUtils;

import java.net.InetAddress;
import java.util.List;
import java.util.Map;

/**
 * Class which represents an individual router and it's associated interfaces and functions
 * @author Cian O'Halloran
 */
public class Router {

    private final InetAddress addr;
    private final String username;
    private final String password;
    private ApiConnection conn;
    private String identity;

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
     * Initializes the router by detecting all interfaces and starting listening for traffic
     * @throws MikrotikApiException If an invalid command is sent to the router
     */
    public void init() throws MikrotikApiException {
        connect();
        fetchIdentity();

        List<Map<String, String>> interfaceDetails = detectInterfaces();
        System.out.println(interfaceDetails);
        for (Map<String, String> singleInterface : interfaceDetails){
            RouterInterface r = createInterface(singleInterface);
            if (r == null) break;
            r.setInitialCost();
            r.listen(conn);
        }
    }

    /**
     * Detects all interfaces on the current router which have an IP address assigned to them.
     * @throws MikrotikApiException If an invalid command is sent to the router
     */
    private List<Map<String, String>> detectInterfaces() throws MikrotikApiException {
        return conn.execute("/ip/address/print");
    }

    /**
     * Parses interface information and creates a corresponding {@link RouterInterface} object, then starts the
     * interface's listening function.
     * @param interfaceResult Map data for a single router interface
     * @throws MikrotikApiException If an invalid command is sent to the router
     */
    private RouterInterface createInterface(Map<String, String> interfaceResult) throws MikrotikApiException {
        String interfaceAddress = interfaceResult.get("address");
        String interfaceLabel = interfaceResult.get("interface");

        int bandwidth = getBandwidth(interfaceAddress);
        if (bandwidth == -1) return null;

        System.out.println(identity + ": Adding interface object for " + interfaceAddress + " on interface "
                + interfaceLabel );
        return new RouterInterface(interfaceLabel, this, identity, bandwidth);
    }

    /**
     * Gets the user inputted bandwidth value for the current link from the config file
     * @param address The IPv4 address in CIDR notation of the interface
     * @return The value (in Mbps) of the maximum bandwidth of the link
     */
    private int getBandwidth(String address) {
        // Create new helper object from IP address part of the address
        address = new SubnetUtils(address).getInfo().getAddress();

        Map<String, Integer> linkBandwidths = Main.getLinkBandwidths();

        // Loop through each network in the config file, and check if the inputted address exists within that subnet
        for (Map.Entry<String, Integer> entry : linkBandwidths.entrySet()) {
            SubnetUtils network = new SubnetUtils(entry.getKey());
            if (network.getInfo().isInRange(address)) {
                System.out.println(identity + ": Bandwidth = " + entry.getValue());
                return entry.getValue();
            }
        }
        // No matching bandwidth value found.
        System.err.println("No valid network for " + address + " found in config file! Assuming client network...");
        return -1;
    }

    /**
     * Executes Mikrotik API commands in a thread safe manner
     * @param cmd The API command to be executed on the router
     * @return The {@code List} of results
     * @throws MikrotikApiException If an invalid command is sent to the router
     */
    public synchronized List<Map<String, String>> execute(String cmd) throws MikrotikApiException{
        // Ensure connection is established before issuing command
        if (conn == null || !conn.isConnected()){
            connect();
        }
        System.out.println("Executing " + cmd);

        return conn.execute(cmd);
    }

    /**
     * Connect to the router using the Mikrotik API
     * @throws MikrotikApiException If an invalid command is sent to the router
     */
    private void connect() throws MikrotikApiException {
        try {
            conn = ApiConnection.connect(addr.getHostAddress());
            conn.login(username, password);
        }catch (InterruptedException e){
            e.printStackTrace();
        }
    }

    /**
     * Gets the identity String for the router for use in output and logging
     * @throws MikrotikApiException If an invalid command is sent to the router
     */
    private void fetchIdentity() throws MikrotikApiException {
        List<Map<String, String>> result = execute("/system/identity/print");
        identity = result.get(0).get("name");
        System.out.println("Setting identity as: " + identity);
    }

    public String getIdentity(){
        return identity;
    }

}
