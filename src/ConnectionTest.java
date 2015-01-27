/**
 * Created by Cian on 18/01/2015.
 */

import me.legrange.mikrotik.ApiConnection;
import me.legrange.mikrotik.MikrotikApiException;

import java.util.List;
import java.util.Map;

public class ConnectionTest {




    public static void main(String[] args) throws MikrotikApiException, InterruptedException{
        ApiConnection con;

        String host = "10.10.100.2";
        String username = "admin";
        String password = "";

        con = ApiConnection.connect(host);
        con.login(username, password);

        con.execute("/routing/ospf/interface/set .id=*6" + " cost=" + 33);

        List<Map<String, String>> result = con.execute("/routing/ospf/interface/print");
        for (Map<String,String> r : result) {
            System.out.println(r);
        }

//        List<Map<String, String>> result = con.execute("/ip/route/print");
//        for (Map<String,String> r : result) {
//            System.out.println(r);
//        }

//        String tag = con.execute("/interface/monitor-traffic interface=ether6",
//                new ResultListener() {
//
//                    public void receive(Map<String, String> result) {
//                        System.out.println(result);
//                    }
//
//                    public void error(MikrotikApiException e) {
//                        System.out.println("An error occurred: " + e.getMessage());
//                    }
//
//                    public void completed() {
//                        System.out.println("Asynchronous command has finished");
//                    }
//
//                }
//        );
//        // let it run for 60 seconds
//        Thread.sleep(60000);
//        con.cancel(tag);
    }

}
