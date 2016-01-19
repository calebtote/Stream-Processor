package cs425.mp1.network;

import com.esotericsoftware.minlog.Log;

import java.io.IOException;
import java.util.ArrayList;


/**
 */

interface RemovePeerListener{
    void PeerRemoved(String hostname);
}

interface AddPeerListener{
    void PeerAdded(String hostname);
}

public class Notifiers {

    public static class RemovePeer implements RemovePeerListener{
        @Override
        public void PeerRemoved(String hostname) {
            Log.warn("Peer:REMOVE", hostname);
            StorageTopology.RemoveNode(hostname);

            if (StorageControl.evaluateMaster() && !StorageControl.getMaster().equals(Node.getLocal().getHostname())) {
                ArrayList<String> params = new ArrayList<String>();
                params.add(Node.getLocal().getHostname());
                StorageControl.sendRPC(StorageControl.getMaster(), StorageControl.RPC_HelloMaster, params);
            }

            if (StorageControl.getMaster().equals(Node.getLocal().getHostname()))
                StorageControl.checkReplication();
        }
    }

    public static class AddPeer implements AddPeerListener{
        @Override
        public void PeerAdded(String hostname){
            Log.info("Peer:ADD", hostname);
            StorageTopology.AddNode(hostname);

            if (StorageControl.evaluateMaster() && !StorageControl.getMaster().equals(Node.getLocal().getHostname())) {
                ArrayList<String> params = new ArrayList<String>();
                params.add(Node.getLocal().getHostname());
                StorageControl.sendRPC(StorageControl.getMaster(), StorageControl.RPC_HelloMaster, params);
            }
        }
    }
}
