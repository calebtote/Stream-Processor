package cs425.mp1.network;

import com.esotericsoftware.minlog.Log;
import cs425.mp1.common.QueryProfile;
import cs425.mp1.common.QueryResults;

import java.util.Map;

/**
 * Purpose:  Generic class for registering classes with Kryonet. These registrations are required
 * in order for proper serialization between hosts via Kryonet sockets.
 * TODO:     Make this more generic and automated. The ability to dynamically register many classes from a static list should be available.
 */
public class Registrar {
    public static void RegisterClass(Node n, Class theClass) {
        Map<String, Node> nodeMap = Node.getNodeMap();
        for (Map.Entry<String, Node> node : nodeMap.entrySet()) {
            if (node.getKey().equals(n.getHostname())) {
                node.getValue().getServer().getConnection().getKryo().register(theClass);
                node.getValue().getClient().getConnection().getKryo().register(theClass);
                Log.trace("Registering " + theClass.getSimpleName() + " for " + node.getValue().getHostname());
            }
        }
    }
}
