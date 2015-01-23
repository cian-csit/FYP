import me.legrange.mikrotik.MikrotikApiException;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;


public class Main {

    public static void main(String[] args) throws UnknownHostException, MikrotikApiException, InterruptedException{

        Properties properties = new Properties();
        InputStream in = null;

        try {
            in = new FileInputStream("config.properties");
            properties.load(in);

        }catch(IOException e){
            System.err.println("Config file not found!");
            return;
        }
        finally{
            if (in != null){
                try {
                    in.close();
                }catch(IOException e){
                    e.printStackTrace();
                }
            }
        }

        List<String> routerStrings = Arrays.asList(properties.getProperty("routers").split("\\s*,\\s*"));

        List<Router> routers = new ArrayList<>();
        for (String s : routerStrings){
            try {
                boolean reachable = InetAddress.getByName(s).isReachable(2000);
                if (reachable) {
                    System.out.println("Creating router at address: " + s);
                    routers.add(new Router(InetAddress.getByName(s)));
                }
                else{
                    System.err.println("Router at " + s + " is not reachable.");
                }
            }catch(IOException e){
                e.printStackTrace();
            }
        }

        for (Router r : routers) start(r);
    }

    public static void start(Router r) throws MikrotikApiException, InterruptedException{
        r.connect();
        r.detectInterfaces();
    }

}
