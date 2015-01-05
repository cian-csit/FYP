/**
 * Created by Cian on 05/11/2014.
 */

import java.util.logging.Level;
import java.util.logging.Logger;

public class ConnectionTest {

    public static void main(String[] args) {
        ApiConn aConn = new ApiConn("192.168.77.2", 8728);
        if (!aConn.isConnected()) {
            System.out.println("Connecting...");
            aConn.start();
            try {
                aConn.join();
                if (aConn.isConnected()) {
                    System.out.println("Connected! Logging in...");
                    aConn.login("admin", new char[0]);
                }
                else{
                    System.out.println("Connection failed!");
                    System.out.println("ApiConn status: " + aConn.getMessage());
                    System.exit(0);
                }
            } catch (InterruptedException ex) {
                Logger.getLogger(ConnectionTest.class.getName()).log(Level.SEVERE, null, ex);
                System.err.println(ex.getMessage());
                System.exit(0);
            }
        }

        aConn.sendCommand("/ip/address/print");
        DataReceiver dataRec = new DataReceiver(aConn);
        dataRec.start();
    }
}
