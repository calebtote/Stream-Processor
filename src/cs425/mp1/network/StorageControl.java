package cs425.mp1.network;

import com.esotericsoftware.minlog.Log;
import cs425.mp1.common.FileControl;
import cs425.mp1.common.FileShard;
import cs425.mp1.main.UX;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Created by ctote on 10/25/15.
 */
public class StorageControl {
    public StorageControl() {
        availableRPCs.put(RPC_HelloMaster, new RPC(RPC_HelloMaster, "(%s): Hello, Master."));
        availableRPCs.put(RPC_KeepAlive, new RPC(RPC_KeepAlive, "KeepAlive"));
        availableRPCs.put(RPC_PutShard, new RPC(RPC_PutShard, "Put: (%s)"));
        availableRPCs.put(RPC_GetShard, new RPC(RPC_GetShard, "%s"+PARAM_SEPARATOR));
        availableRPCs.put(RPC_AckShard, new RPC(RPC_AckShard, "AckShard: (%s)"));
        availableRPCs.put(RPC_AckFile, new RPC(RPC_AckFile, "AckFile: (%s)"));
        availableRPCs.put(RPC_ValidateFile, new RPC(RPC_ValidateFile, "%s"+PARAM_SEPARATOR+"%s"+PARAM_SEPARATOR+"%s"+PARAM_SEPARATOR));    //hacky -- filename, filesize, shardcount
        availableRPCs.put(RPC_RequestShardList, new RPC(RPC_RequestShardList, "%s"));
        availableRPCs.put(RPC_ListShards, new RPC(RPC_ListShards, "%s"));
        availableRPCs.put(RPC_RequestStorageView, new RPC(RPC_RequestStorageView, "ReqStorageView: (%s)"));
        availableRPCs.put(RPC_StorageView, new RPC(RPC_StorageView, "%s"));
        availableRPCs.put(RPC_ReplicateShard, new RPC(RPC_ReplicateShard, "%s" + PARAM_SEPARATOR + "%s" + PARAM_SEPARATOR));  // host;shard

        // crane RPCs
        availableRPCs.put(RPC_CraneWorkerInit, new RPC(RPC_CraneWorkerInit, "%s" + PARAM_SEPARATOR + "%s")); // coordintor;utility
        availableRPCs.put(RPC_CraneWorkerNextHop, new RPC(RPC_CraneWorkerNextHop, "%s"));
        availableRPCs.put(RPC_CraneEnd, new RPC(RPC_CraneEnd, "CraneEnd"));
    }

    public static final String RPC_HelloMaster = "helloMaster";
    public static final String RPC_KeepAlive = "keepAlive";
    public static final String RPC_PutShard = "putShard";
    public static final String RPC_GetShard = "getShard";
    public static final String RPC_AckShard = "ackShard";
    public static final String RPC_AckFile = "ackFile";
    public static final String RPC_ValidateFile = "validateFile";
    public static final String RPC_RequestShardList = "requestShardList";
    public static final String RPC_ListShards = "listShards";
    public static final String RPC_RequestStorageView = "reqStorageView";
    public static final String RPC_StorageView = "storageView";
    public static final String RPC_ReplicateShard = "replicateShard";

    // crane RPCs
    public static final String RPC_CraneWorkerInit = "craneWorkerInit";
    public static final String RPC_CraneWorkerNextHop = "craneWorkerNextHop";
    public static final String RPC_CraneEnd = "craneEnd";


    public static final String PARAM_SEPARATOR = ";";
    private static boolean canProceed = true;
    private static Object returnObject = null;
    private static long maxWait = 3500; // 3.5sec
    private static long ackWait = 50; // 50ms
    private static long maxAttempts = 3;
    private static String MasterHostname;

    public static String getMaster() {
        return MasterHostname;
    }

    public static void setMaster(String host) {
        MasterHostname = host;
    }

    // storageTopology is sorted based on Node ID
    // first entry should always be the `Master`

    /**
     * Check if the current master is the first node in the list of nodes in the topology
     *
     * @return true if current master got changed, false if the current master is the right leader
     */
    public static boolean evaluateMaster() {
        if (!StorageTopology.getLeaderNode().equals(getMaster())) {
            setMaster(StorageTopology.getLeaderNode());
            return true;
        }
        return false;
    }

    private static Map<String, RPC> availableRPCs = new HashMap<String, RPC>();

    public static RPC getRPC(String key, ArrayList<String> p) {
        return new RPC(availableRPCs.get(key).tag, String.format(availableRPCs.get(key).getMsg(), p.toArray(new String[p.size()])));
       // rawRPC.setMsg(String.format(rawRPC.getMsg(), p.toArray(new String[p.size()])));
    }

    public static void sendRPC(String host, String key, ArrayList<String> p) {
        RPC rawRPC;

        if (p.size() > 0) {
            rawRPC = new RPC(availableRPCs.get(key).tag, String.format(availableRPCs.get(key).getMsg(), p.toArray(new String[p.size()])));
            rawRPC.setMsg(String.format(rawRPC.getMsg(), p.toArray(new String[p.size()])));
        }
        else
            rawRPC = StorageControl.availableRPCs.get(key);

        Node.sendTCP(Node.getNodeByHostname(host), rawRPC);
    }

    // Contains mapping of known files to shards we've stored locally
    private static Map<String, ArrayList<FileShard>> fileShardMap = new HashMap<String, ArrayList<FileShard>>();

    // Contains mapping of known files to shards for all nodes
    private static Map<String, Map<String, ArrayList<FileShard>>> globalFileShardMap = new HashMap<String, Map<String, ArrayList<FileShard>>>();
    public static Map<String, Map<String, ArrayList<FileShard>>> getGlobalFileShardMap() {
        return globalFileShardMap;
    }

    public static Map<String, ArrayList<FileShard>> getFileShardMap() {
        return fileShardMap;
    }

    public static ArrayList<FileShard> getFileShards(String fileName) {
        if (fileShardMap.containsKey(fileName)) {
            return fileShardMap.get(fileName);
        }
        return null;
    }

    public static void trackShard(String filename, FileShard shard) {

        // Update our local file tracker
        // Can probably be done away with, with the addition of the global file tracker
        // But that thing is a pain to traverse.. so keeping this around for now.
        if (fileShardMap.containsKey(filename)) {
            Log.info("Shard", "Tracking shard: " + shard.getSeqNum() + " for file: " + filename);
            fileShardMap.get(filename).add(shard);
        } else {
            Log.info("Shard", "Tracking new file: " + filename);
            ArrayList<FileShard> initShard = new ArrayList<FileShard>();
            initShard.add(shard);
            fileShardMap.put(filename, initShard);
        }

        // Update our local Node:File:Shards tracker
        if (globalFileShardMap.containsKey(Node.getLocal().getHostname())) {
            if (globalFileShardMap.get(Node.getLocal().getHostname()).containsKey(filename)) {
                globalFileShardMap.get(Node.getLocal().getHostname()).get(filename).add(shard);
            } else {
                ArrayList<FileShard> initShard = new ArrayList<FileShard>();
                initShard.add(shard);
                globalFileShardMap.get(Node.getLocal().getHostname()).put(filename, initShard);
            }
        }
        else{
            ArrayList<FileShard> initShard = new ArrayList<FileShard>();
            initShard.add(shard);

            Map<String, ArrayList<FileShard>> initMap = new HashMap<String, ArrayList<FileShard>>();
            initMap.put(filename,initShard);

            globalFileShardMap.put(Node.getLocal().getHostname(), initMap);
        }
    }


    public static void mergeFileFromShards(String orignalFileName, String newFileName) {
        try {
            ArrayList<FileShard> shards = new ArrayList<FileShard>();
            // 1. Query each peer for the list parts for the concerned file
            //  and collect a map of parts -> peers
            Map<String, ArrayList<String>> partPeerMap = queryPeersForParts(orignalFileName);
            // 2. part1, ask peer1 to send it and save. If peer1 does not respond, ask peer2, and so on.
            for (String shardFileName : partPeerMap.keySet()) {
                // 3. Do for all parts
                ArrayList<String> peerList = partPeerMap.get(shardFileName);
                for (String peer : peerList) {

                    RPC rawRPC = availableRPCs.get(RPC_GetShard);
                    rawRPC.setMsg(String.format("%s"+PARAM_SEPARATOR, shardFileName));
                    // wait until the peer replies back with the shard
                    // if it doesn't, go the next peer in the peer list
                    if(sendBlocking(peer, rawRPC)) {
                        shards.add((FileShard) returnObject);
                        break;
                    }
                }
            }
            // 4. Construct a new file out of the parts
            FileControl.mergeFiles(shards, newFileName);
        } catch (Exception ex) { ex.printStackTrace();}
    }

    public static void checkReplication() {
        Log.warn("CheckReplication", "Checking...");
        Map<String, Integer> shardCounts = new HashMap<String, Integer>();
        ArrayList<String> shardsToReplicate = new ArrayList<String>();
        Map<String, String> replicationPair = new HashMap<String, String>();

        // Replicating Host : (Source Host : Shard)
        Map<String, Map<String, String>> replicationTransaction = new HashMap<String, Map<String, String>>();

        for (String filename : fileShardMap.keySet()){
            Map<String, ArrayList<String>> partPeerMap = queryPeersForParts(filename);
            for (String shard : partPeerMap.keySet()){
                if (shardCounts.containsKey(shard)){
                    int curCount = shardCounts.get(shard);
                    shardCounts.put(shard, curCount+1);
                }
                else
                    shardCounts.put(shard, 1);


                for (String shard_x : shardCounts.keySet()){
                    if (shardCounts.get(shard) < 3){
                        Log.info("Replication", "Adding shard to list: " + shard);
                        shardsToReplicate.add(shard_x);
                    }
                }

                for (String shard_y : shardsToReplicate){
                    for (String host : StorageTopology.GetStorageTopology().values()){
                        // Skip the master; check that `host` doesn't already own a copy
                        if (partPeerMap.get(shard_y).contains(host))
                            continue;
                        else{
                            for (String h : partPeerMap.get(shard_y)){
                                // Same host, no good.
                                if (h.equals(host))
                                    continue;

                                // Host is dead
                                else if (!StorageTopology.GetStorageTopology().containsValue(h))
                                    continue;

                                // Tell `host` to replicate `shard` from `h`
                                else{
                                    Log.info("Replication", String.format("New transaction: %s >> (%s:%s)", host, h, shard));
                                    replicationPair.put(h, shard);
                                    replicationTransaction.put(host, replicationPair);
                                    break;
                                }
                            }
                        }
                    }
                }
            }
        }

        for (String host : replicationTransaction.keySet()){
            String sourceHost = replicationTransaction.get(host).keySet().iterator().next();
            String shard = replicationTransaction.get(host).values().iterator().next();

            ArrayList<String> params = new ArrayList<String>();
            params.add(sourceHost);
            params.add(shard);

            Log.info("Replication", String.format("Requesting %s be replicated to %s via %s", shard, host, sourceHost));
            Node.sendTCP(Node.getNodeByHostname(host), StorageControl.getRPC(StorageControl.RPC_ReplicateShard, params));
        }

    }


    /**
     * Passes on all peers in the topology, and gets the list of parts relating to a particular file
     * A loop with all peers in the topology
     * 1. Go the peer, get the list of parts for the file
     * 2. Add the peer name to the ArrayList in front of each key corresponding to the part sequence
     * @param fileName
     * @return a map with the list of parts names (as keys) and list of all the nodes that have them
     */
    public static Map<String, ArrayList<String>> queryPeersForParts(String fileName) {
        HashMap<String, ArrayList<String>> partsMap = new HashMap<String, ArrayList<String>>();

        RPC rawRPC = availableRPCs.get(RPC_RequestShardList);
        rawRPC.setMsg(String.format("%s"+PARAM_SEPARATOR, fileName));

        String peer = StorageTopology.GetInitiator();
        String first = peer;

        do {
            peer = StorageTopology.getNextPeer(peer);
            //Node.sendTCP(Node.getNodeByHostname(peer), rawRPC);
            String shardList  = "";
            // get back the reply from the peer
            if(sendBlocking(peer, rawRPC)) {
                // shardList is a string of the form filex.ext.part1;filex.ext.part4.. or empty string
                shardList = (String) returnObject;

                Log.warn("QueryPeersForParts","Found on "+peer + " the following shards: "+shardList);
                // pass through the list of tokens, search in the array list, if not there add it
                // add the peer name to the array list of peers for this part
                String[] st = shardList.split(PARAM_SEPARATOR);
                for (int i = 0; i < st.length; i++) {
                    String part = st[i];
                    ArrayList<String> peers = null;
                    if (partsMap.containsKey(part)) {
                        peers = partsMap.get(part);
                    } else {
                        peers = new ArrayList<String>();
                        partsMap.put(part, peers);
                    }
                    peers.add(peer);
                }
            }

        } while (!peer.equals(first));

        return partsMap;
    }

    /** Receives a file to shard and distributes it sequentially to all the nodes according the set algorithm.
     * If you are the master, continue, else send the file to the master
     * 1. Shard the file
     * 2. Create a new storage controller with the initiator as the current file sequence (ring based)
     * 3. Store the first shard in nodes; initiator, initiator + 1, initiator + 2
     * 4. Next shard in initiator + shard_id, initiator + shard_id + 1, initiator + shard_id + 2
     * 5. Note, node ids are arranged in ring topology
     *
     * @param filePath
     * @throws Exception
     */
    public static void initiateFileDistribution(String filePath) throws Exception {
        List<FileShard> fileShards = FileControl.ShardFile(filePath);
        String peer = StorageTopology.GetInitiator();

        // stop heartbeats temporarily
        boolean respondingMode = Node.isRespondingMode();
        //Node.setRespondingMode(false);
        UX.PauseTimerTask();
        for (FileShard shard : fileShards){
            sendShard(peer, shard);

            String peerWithCopy1 = StorageTopology.getNextPeer(peer);
            sendShard(peerWithCopy1, shard);

            String peerWithCopy2 = StorageTopology.getNextPeer(peerWithCopy1);
            sendShard(peerWithCopy2, shard);

            // go to next peer
            peer = StorageTopology.getNextPeer(peer);
        }
        UX.StartTimerTask();
        //Node.setRespondingMode(respondingMode);
        //StorageTopology.ShiftInitiator();
    }

    public static List<FileShard> uploadFileToMaster(String filePath) throws Exception {
        List<FileShard> fileShards = FileControl.ShardFile(filePath);

        // stop heartbeats temporarily
        boolean respondingMode = Node.isRespondingMode();
      //  Node.setRespondingMode(false);

        for (FileShard shard : fileShards) {
            int attempts = 0;
            while (attempts <= maxAttempts) {
                attempts++;
                if (sendShard(StorageControl.getMaster(), shard)) break;
                else
                    Log.error("Upload", String.format("Failed to upload %s. Attempt [%s/%s].", filePath, attempts, maxAttempts));
                }
            }

       // Node.setRespondingMode(respondingMode);
        return fileShards;
    }

    private static boolean sendShard(String hostName, FileShard shard) {

        //skip self (master) for now..
        if (hostName.equals(Node.getLocal().getHostname()))
            return true;

        Log.warn("Send", "Sending on TCP " + shard.getFileName() + FileShard.SHARD_PART_TAG + shard.getSeqNum() + " to " + hostName);
        Log.debug("ShardSize", String.valueOf(shard.getPayload().length));

        return sendBlocking(hostName, shard);
    }


    /** sends the input to the target host name, blocks until it gets back ar reply.
     * The reply will be stored in the return object
     * @param hostName
     * @param input
     * @return true if it succeeds
     */
    private static boolean sendBlocking(String hostName, Object input) {

        //Log.warn("Send", "Sending on TCP " + shard.getFileName() + FileShard.SHARD_PART_TAG + shard.getSeqNum() + " to " + hostName);
        // get ack
        try {
            resetReturnObject();
            canProceed = false;
            Node.getLocal().sendTCP(Node.getNodeByHostname(hostName), input);

            long ackTime = 0;
            int attempt = 1;
            while (!canProceed && ackTime <= maxWait) {
                ackTime += ackWait;
                Log.debug("AckWait", String.format("Current ackTime for input (%s): %s", input.toString(), ackTime));
                //Log.debug("AckWait", String.format("Current ackTime for input (%s): %s", input.toString(), ackTime));
                Thread.sleep(ackWait);
            }
            if (ackTime > maxWait){
                Log.debug("AckWait", String.format("Maximum Ack Wait time (%s) exceeded for input: %s!", maxWait, input.toString()));
                return false;
            }
            else{
                Log.info("AckTime", String.valueOf(ackTime));
            }
        } catch (Exception ex) {ex.printStackTrace();}
        return true;
    }


    public  static  void youCanProceed() { canProceed = true;}
    public static boolean canProceed() {return canProceed;}
    public static void canProceed(boolean var) {canProceed = var;}
    public static void setReturnObject(Object _returned) {returnObject = _returned;}
    public static void resetReturnObject() {returnObject = null;}
}
