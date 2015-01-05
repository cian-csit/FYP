
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author janisk
 */
public class DataReceiver extends Thread {

    private ApiConn aConn = null;

    public DataReceiver(ApiConn aConn) {
        this.aConn = aConn;
    }

    @Override
    public void run() {
        while (true) {
            try {
                String s = aConn.getData();
                if (s != null) {
                    System.out.println(s);
                }
            } catch (InterruptedException ex) {
                Logger.getLogger(DataReceiver.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
}
