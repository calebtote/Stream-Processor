package cs425.mp1.network;

import com.esotericsoftware.minlog.Log;
import cs425.mp1.main.ConfigManager;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 *
 */
public class PeerGroup {
    PeerGroup(){
        Notifiers.RemovePeer rmPeerNotify = new Notifiers.RemovePeer();
        peerRemovedListeners.add(rmPeerNotify);

        Notifiers.AddPeer addPeerNotify = new Notifiers.AddPeer();
        peerAddedListeners.add(addPeerNotify);

        try{
            StorageTopology.AddNode(InetAddress.getLocalHost().getHostName());
        } catch (UnknownHostException e) { e.printStackTrace();}
    }

    List<RemovePeerListener> peerRemovedListeners = new ArrayList<RemovePeerListener>();
    List<AddPeerListener> peerAddedListeners = new ArrayList<AddPeerListener>();




    public final static long PEER_MAXAGE = Integer.parseInt(ConfigManager.getProperties().getProperty("PeerMaxAge"));
    private static PeerGroup instance;
    public static PeerGroup getInstance() {
        if(instance == null) instance = new PeerGroup();
        return instance;
    }

    // The set of all Peers connected to this host, hashed by the hostname
    private Map<String, PeerConnection> Peers = new HashMap<String, PeerConnection>();
    public Map<String, PeerConnection> getPeers() {
        return Peers;
    }

    public void RemovePeer(String hostname) {
        Peers.remove(hostname);

        // notify all listeners
        for (RemovePeerListener pl : this.peerRemovedListeners){
            pl.PeerRemoved(hostname);
        }
    }

    /**
     * For each node in the list of nodes in the system
     * 1. If it is me, don't do anything,
     * 2. If it is one of my peers, don't do anything
     * 3. Else, try to peer with that node
     */
    public void PeerWithNodes() {
        Map<String, Node> nodeMap = Node.getNodeMap();
        if (nodeMap.size() - 1 > getPeers().size()) {   // -1 since the node map contains the local host as one of the entries
            Log.trace("[MP-CS425] -- Looking for new nodes to peer with");
            for (Map.Entry<String, Node> entry : nodeMap.entrySet()) {
                if (!entry.getValue().isItMe() && !isPeeredWith(entry.getKey())) {
                    peerWithNode(entry.getValue());
                }
            }
        } else {
            Log.trace("[MP-CS425] -- All group nodes are in the peer list!");
        }
    }

    private void peerWithNode(Node node) {
        Node.getLocal().getClient().Start();
        Log.trace("[MP-CS425] -- Trying ..  " + node.getHostname());
        if (Node.getLocal().getClient().SendUDP(node, "hello!") > 0) {
            addPeer(node);
        } else {
            Log.debug("[MP-CS425] -- " + node.getHostname() + " checked and appears to be down!");
        }

    }

    /**
     * Checks if the passed is part of the list of peers
     *
     * @param hostname
     * @return
     */
    public boolean isPeeredWith(String hostname) {
        boolean alreadyPeered = false;
        Map<String, PeerConnection> peerMap = getPeers();
        for (Map.Entry<String, PeerConnection> peer : peerMap.entrySet()) {
            if (peer.getValue().getPeerHostname().equals(hostname)) {
                alreadyPeered = true;
                break;
            }
        }
        return alreadyPeered;
    }


    /**
     * adds the node to the list of peers
     *
     * @param node
     */
    public void addPeer(Node node) {

//        entry.getClient().Start();
        if (!isPeeredWith(node.getHostname())) {
            //PeerGroup.AddPeer(new PeerConnection(entry.getValue().getClient().getConnection().getRemoteAddressUDP()));
            PeerConnection peer = new PeerConnection(node);
            Peers.put(node.getHostname(), peer);
            Log.debug(" Current Members:\n" + EnumeratePeers());

            // notify all listeners
            for (AddPeerListener pl : this.peerAddedListeners){
                pl.PeerAdded(node.getHostname());
            }
        }
    }

    /**
     * Goes through all the known peers, sends a heartbeat to check if it is still alive
     * returns list of stale connections
     *
     * @return
     */
    public List<PeerConnection> CheckPeers() {

        if(getPeers().size() > 0)
            Log.trace("Peer:CHECK", " -- Entering Checking Current Peers Status");

        // check the full set

            return CheckPeers(getPeers());
    }



    private List<PeerConnection> CheckPeers(Map<String, PeerConnection> _peersList) {

        List<PeerConnection> PeersMarkedForDelete = new ArrayList<PeerConnection>();
        if(_peersList.size() > 0)
            Log.trace("Peer:CHECK", " -- Entering Checking Current Peers Status");

        Iterator it = _peersList.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, PeerConnection> item = (Map.Entry<String, PeerConnection>) it.next();
            Log.trace("Peer", String.format("Current age of %s: %s", item.getValue().getPeerHostname(), String.valueOf(String.format("%dms [Heartbeat Check at %sms]", item.getValue().getPeerUpdateAge(), String.valueOf(PEER_MAXAGE)))));
            if (!Node.inTransactionWith(item.getValue().getPeerHostname())) {
                if (Node.getLocal().getClient().SendTCP(item.getValue().getPeer(), StorageControl.getRPC(StorageControl.RPC_KeepAlive, new ArrayList<String>())) < 0)
                    PeersMarkedForDelete.add(item.getValue());

//                if (item.getValue().getPeerUpdateAge() > PEER_MAXAGE) {
//                    if (SendHeartbeat(item.getValue()) <= 0) {
//                        Log.trace("Peer", item.getKey() + " appears to be down!");
//                        PeersMarkedForDelete.add(item.getValue());
//                    } else {
//                        Log.trace("Peer", " -- " + item.getKey() + " appears to be Up!");
//                        Log.trace("Peer", " --  Updating the peer update age");
//                        item.getValue().UpdateLastCheckDate();
//                    }
//                }
            }
        }

        for (PeerConnection p : PeersMarkedForDelete){
                this.RemovePeer(p.getPeerHostname());
        }

        return PeersMarkedForDelete;
    }

    /**
     * Sends a heartbeat messages, and sends it to the named peer
     * returns the number of acknowledgements got back
     *
     * @param peer
     * @return
     */
    private static int SendHeartbeat(PeerConnection peer) {
        int acks = 0;
        Heartbeat hb = peer.GenerateHeartbeat();
        hb.setMsg(hb.getBeat());

        while ((acks <= 0) && (hb.missedBeats < hb.MAX_MISS)) {
            try {
                if (hb.missedBeats > 0) Thread.sleep(1000);

                hb.missedBeats++;
                Log.trace("Peer:HEARTBEAT", String.format(" -- Sending heartbeat [%d/%d] to %s..", hb.missedBeats, hb.MAX_MISS, peer.getPeerHostname()));
                acks = Node.sendUDP(peer.getPeer(), hb);

                // share the list
                //acks = Node.getLocal().getClient().SendUDP(peer.getPeer(), getInstance());

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (acks <= 0) {
            Log.debug("Peer:HEARTBEAT", String.format(" -- %d of %d heartbeats failed for peer: %s", hb.missedBeats, hb.MAX_MISS, peer.getPeerHostname()));
        }
        peer.SetActiveHeartbeat(hb);
        return acks;
    }

    public String EnumeratePeers() {

        if (getPeers().isEmpty())
            return "None";

        StringBuffer output = new StringBuffer();
        DateFormat dateFormat = new SimpleDateFormat("HH:mm:ss.SSS");
        Map<String, PeerConnection> peerMap = getPeers();
        for (Map.Entry<String, PeerConnection> entry : peerMap.entrySet())
            output.append("[" + entry.getValue().getID() + "\n");

        return output.toString();
    }

    /*
    public List<PeerConnection> HandleSharedPeerGroup(PeerGroup group) {
        // if a peer is common and checked later than my check, update the last checked time
        // if a peer is missing from my peer list (not me), check if it is up and add it
        // if a peer is not in their list (and not me), check if it is still up and remove it if it is not responding
        Map<String, PeerConnection> missedPeers = getPeersCopy();

        for (Map.Entry<String, PeerConnection> entry : group.getPeers().entrySet()) {
            if(!entry.getValue().isItMe()) {    // I am not checking or peering with myself
                if (getPeers().containsKey(entry.getKey())) {
                    PeerConnection myEntry = getPeers().get(entry.getKey());
                    if (myEntry.getPeerLastUpdate().before(entry.getValue().getPeerLastUpdate())) {
                        myEntry.setPeerLastUpdate(entry.getValue().getPeerLastUpdate());
                    }
                } else peerWithNode(Node.getNodeByHostname(entry.getKey()));
            }
            if(missedPeers.containsKey(entry.getKey())) missedPeers.remove(entry.getKey());
        }
        // hmmm.. wondering about the peers missed from the shared list, are they still up?
        return CheckPeers(missedPeers);
    }*/

    private Map<String, PeerConnection> getPeersCopy() {
        Map<String, PeerConnection> copy = new HashMap<String, PeerConnection>();

        for (Map.Entry<String, PeerConnection> peer : getPeers().entrySet()) {
            copy.put(peer.getKey(), peer.getValue());
        }
        return copy;
    }
}
