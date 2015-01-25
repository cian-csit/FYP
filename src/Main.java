import me.legrange.mikrotik.MikrotikApiException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;

public class Main {

    private static List<Router> routers;
    private static Map<String, Integer> linkBandwidth;

    public static void main(String[] args) throws UnknownHostException, MikrotikApiException, InterruptedException{

        routers = new ArrayList<>();
        linkBandwidth = new HashMap<>();

        parseXML();

        for (Router r : routers) start(r);
    }

    public static void start(Router r) throws MikrotikApiException, InterruptedException{
        r.connect();
        r.detectInterfaces();
    }

    public static void addRouter(String address, String username, String password){
        try {
            boolean reachable = InetAddress.getByName(address).isReachable(2000);
            if (reachable) {
                System.out.println("Creating router at address: " + address);
                routers.add(new Router(InetAddress.getByName(address), username, password));
            }
            else{
                System.err.println("Router at " + address + " is not reachable.");
            }
        }catch(IOException e){
            e.printStackTrace();
        }
    }

    public static Map<String, Integer> getLinkBandwidth(){
        return linkBandwidth;
    }

    public static void parseXML(){
        try{
            DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
            Document doc = docBuilder.parse(new File("config.xml"));

            // Normalize text
            doc.getDocumentElement().normalize();

            NodeList listOfRouters = doc.getElementsByTagName("router");
            int numRouters = listOfRouters.getLength();
            System.out.println("Number of routers: " + numRouters);

            // Loop through each router node
            for (int i=0; i<listOfRouters.getLength(); i++){
                Node router = listOfRouters.item(i);
                if (router.getNodeType() == Node.ELEMENT_NODE){

                    Element routerElement = (Element)router;

                    // Address child node
                    Element addressElement = (Element)routerElement.getElementsByTagName("address").item(0);
                    String address = addressElement.getChildNodes().item(0).getNodeValue();

                    // Username child node
                    Element usernameElement = (Element)routerElement.getElementsByTagName("username").item(0);
                    String username = usernameElement.getChildNodes().item(0).getNodeValue();

                    // Password child node
                    Element passwordElement = (Element)routerElement.getElementsByTagName("password").item(0);
                    String password;
                    if (passwordElement.hasChildNodes()) {
                        password = passwordElement.getChildNodes().item(0).getNodeValue();
                    }
                    else {
                        password = "";
                    }

                    // Create router object
                    addRouter(address, username, password);
                }
            }

            NodeList listOfLinks = doc.getElementsByTagName("link");
            int numLinks = listOfLinks.getLength();
            System.out.println("Number of links: " + numLinks);

            // Loop through each link node
            for (int i=0; i<listOfLinks.getLength(); i++){
                Node link = listOfLinks.item(i);
                if (link.getNodeType() == Node.ELEMENT_NODE){

                    Element linkElement = (Element)link;

                    // Link address child node
                    Element linkAddressElement = (Element)linkElement.getElementsByTagName("address").item(0);
                    String address = linkAddressElement.getChildNodes().item(0).getNodeValue();

                    // Bandwidth child node
                    Element bandwidthElement = (Element)linkElement.getElementsByTagName("bandwidth").item(0);
                    int speed = Integer.parseInt(bandwidthElement.getChildNodes().item(0).getNodeValue());

                    linkBandwidth.put(address, speed);
                }
            }
        }catch (IOException | ParserConfigurationException | SAXException e) {
            e.printStackTrace();
        }
    }

}
