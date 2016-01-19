package cs425.mp1.network;

import com.esotericsoftware.minlog.Log;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Created by ctote on 10/29/15.
 */
public class StorageTopology {

    // The logical topology for how we map our nodes to files
    // 0th entry is always the `Master`
    private static Map<Integer,String> storageTopology = new TreeMap<Integer,String>();
    public static Map<Integer, String> GetStorageTopology() {
        return storageTopology;
    }

    public static void AddNode(String hostname){
        System.out.println("Adding "+hostname+" to the storage topology");
        storageTopology.put(Integer.valueOf(hostname.substring(15, 17)), hostname);
    }
    public static boolean RemoveNode(String hostname){
        Integer key = Integer.valueOf(hostname.substring(15, 17));

        if (!storageTopology.get(key).equals(hostname)){
            // this is not the node we're looking for
            return false;
        }

        try{
            if (!Node.getLocal().getHostname().equals(storageTopology.get(key))) {
                storageTopology.remove(key);
                return true;
            }
            else
                Log.debug("Peer:REMOVE", "Denying request to remove localhost from PeerGroup::storageTopology!");
        }catch (NullPointerException ex){
            Log.debug("Peer:REMOVE", String.format("Peer key %s for host %s not in PeerGroup::storageTopology!", key.toString(), hostname));
        }

        // something else failed
        return false;
    }

    private static Integer currentInitiator;
    public static void SetInitiator(Integer id){
        if(storageTopology.containsKey(id)){
            Log.trace("Storage", String.format("Set initiator node to: %s", storageTopology.get(id)));
            currentInitiator = id;
        }
        else
            Log.warn("Storage", String.format("Attempting to set initiator node to invalid id: %s", id));
    }

    public static String GetInitiator(){
        //return storageTopology.get(currentInitiator);
        //TODO: fix the hard-coded return
        return "fa15-cs425-g27-01.cs.illinois.edu";
    }

    // shift initiator to next available node in the storage topology
    public static void ShiftInitiator(){
        Iterator it = storageTopology.keySet().iterator();
        while (!it.next().equals(currentInitiator)){
            if (it.hasNext())
                it.next();
            else it = storageTopology.keySet().iterator();
        }
        if (it.hasNext()) {
            Log.trace("Storage", String.format("Setting initiator node to: %s", it.next()));
            SetInitiator((Integer) it.next());
        }
        else {
            it = storageTopology.keySet().iterator();
            Log.trace("Storage", String.format("Setting initiator node to: %s", it.next()));
            SetInitiator((Integer) it.next());
        }
    }

    public static String getLeaderNode() {
        return StorageTopology.GetStorageTopology().entrySet().iterator().next().getValue();
    }

    /** returns the next peer in the storage topology
     * If the currentPeer is the last one in the ring, return the first one
     */
    public static String getNextPeer(String current) {
        String peer = null;
        //boolean appendDomain = false;
        Iterator it = storageTopology.values().iterator();

        // should use only short hostnames for search
        if(current.indexOf('.') > 0) {
            current = current.substring(0, current.indexOf('.'));
            //appendDomain = true;
        }

        while (it.hasNext()) {
            String nextPeer = (String) it.next();
            //Log.info("StorageTopology", "Comparing "+nextPeer+" with "+current);
            if (nextPeer.startsWith(current))
                // found a match, get the next in line
                if(it.hasNext()) {
                    peer = (String) it.next();
                    break;
                } else {
                    // terminal node, get the first node as next
                    Iterator i =  storageTopology.values().iterator();
                    peer = (String) i.next();
                }
        }
        //peer = (appendDomain)? peer + ".cs.illinois.edu": peer;
        return peer;
    }


    public static void main(String args[]) {
        String hostname = "fa15-cs425-g27-0";
        for (int i = 0; i < 4; i++) {
            AddNode(hostname+(i+1));
        }

        System.out.println(getNextPeer("fa15-cs425-g27-0"+1));
        System.out.println(getNextPeer("fa15-cs425-g27-0"+1)+".cs.illinois.edu");
        System.out.println(getNextPeer("fa15-cs425-g27-0"+2));
        System.out.println(getNextPeer("fa15-cs425-g27-0"+4));

    }
}
