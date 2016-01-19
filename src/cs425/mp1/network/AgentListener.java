package cs425.mp1.network;

import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.FrameworkMessage;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.minlog.Log;
import cs425.mp1.agent.QueryHandler;
import cs425.mp1.common.*;
import cs425.mp1.crane.*;
import cs425.mp1.crane.apps.AppBase;


import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 */
public class AgentListener extends Listener {
    public void connected(Connection connection) {
//        Map<InetSocketAddress, PeerConnection> peerMap = PeerGroup.getPeers();
//        for (Map.Entry<InetSocketAddress, PeerConnection> entry : peerMap.entrySet()){
//            if (entry.getKey() == connection.getRemoteAddressUDP()) {
//                System.out.println("Already connected to " + connection.getRemoteAddressUDP());
//                return;
//            }
//        }
//
//        Node correspondingNode = Node.getCorrespondingNode(connection);
//        PeerGroup.addPeer(correspondingNode);

        Log.trace("Connect", "Connection Event in the Agent Listener");


//        PeerGroup.AddPeer(new PeerConnection(connection.getRemoteAddressUDP()));
//        System.out.println("----------> New Client Connected  <--------------");
//        System.out.println(" Current Members:\n" + PeerGroup.EnumeratePeers());
    }

    public void received(Connection connection, Object object) {
        if (!(object instanceof FrameworkMessage.KeepAlive) &&
                (!(object instanceof RPC) || (object instanceof RPC && !((RPC) object).tag.equals(StorageControl.RPC_KeepAlive)))) {
            if (object instanceof  RPC) Log.info(((RPC)object).tag);
                Log.info("Transaction", String.format("Starting transaction with Node: %s", connection.getRemoteAddressTCP().getHostName()));
                Log.info("Transaction", String.format("Object: %s", object.getClass().getSimpleName()));
                Node.startTransaction(connection.getRemoteAddressTCP().getHostName());
            }

        if (object instanceof StreamWrapper) {
            StreamWrapper sw = (StreamWrapper) object;

            if (WorkerControl.getCoordinator().getHostname().equals(Node.getLocal().getHostname())){
                //tmp
                Log.info("Stream returned!");
                for (String s : sw.getWorkingDeque()) {
                    Log.info(s);
                }
            }
            else {
                // WorkerControl.getGateKeeper().EnqueueWork(sw);
                EnQThread enq = new EnQThread(sw);
                enq.start();
                try {
                    enq.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        if (object instanceof AppBase){
            WorkerControl.setApp((AppBase) object);
        }

        if (object instanceof QueryProfile) {
            QueryResults response = null;
            try {
                QueryProfile queryProfile = (QueryProfile) object;
                QueryHandler grep = QueryHandler.getQueryHandler();
                grep.handleQuery(queryProfile, queryProfile.getQuery());
                System.out.println("Request from "+connection.getRemoteAddressTCP()+"\n\tSearching " + queryProfile.getFileName() + " for " + queryProfile.getQuery());
                response = grep.getQueryResults();
            } catch (QueryException e) {
                e.printStackTrace();
            } catch (IOException e) {e.printStackTrace();}
            connection.sendTCP(response);
        }

        if (object instanceof Heartbeat && Node.isRespondingMode()) {
            Heartbeat hb = (Heartbeat) object;
            if (hb.msg.equals(hb.beat)){
                hb.msg = hb.ack;
                Log.trace("Heartbeat", String.format(" -- Got beat from %s, sending ack.", connection.getRemoteAddressUDP().getHostName()));
                connection.sendUDP(hb);
            }
            else{
                Log.trace("Heartbeat", String.format("Got ack from %s.", connection.getRemoteAddressUDP().getHostName()));
            }
        }
        // sharing the current peer group
       // if (object instanceof PeerGroup && Node.isRespondingMode()) {
       //     PeerGroup.getInstance().HandleSharedPeerGroup((PeerGroup)object);
       // }

        if (object instanceof RPC){
            RPC rpc = (RPC) object;
            if (!rpc.tag.equals(StorageControl.RPC_KeepAlive)) {
                Log.info("RPC", rpc.tag);
                Log.info("RPC", rpc.getMsg());
            }

            if (rpc.tag.equals(StorageControl.RPC_StorageView)){
                String[] fileList = rpc.getMsg().split(StorageControl.PARAM_SEPARATOR);
                for (String file : fileList){
                    Log.info("ListFiles", String.format("%s: %s", connection.getRemoteAddressTCP().getHostName(), file));
                }
            }
            else if (rpc.tag.equals(StorageControl.RPC_ListShards)){
                // the reply to RequestShardList
                // gets the list of shards contained on this node for a given fileName
                //

            }
            else if (rpc.tag.equals(StorageControl.RPC_RequestStorageView)){
                String localFiles = "";
                for (String file : StorageControl.getFileShardMap().keySet()){
                    localFiles += String.format("%s"+StorageControl.PARAM_SEPARATOR, file);
                }

                ArrayList<String> params = new ArrayList<String>();
                params.add(localFiles);
                Log.info(localFiles);

                StorageControl.canProceed(false);
                connection.sendTCP(StorageControl.getRPC(StorageControl.RPC_StorageView, params));
            }
            else if (rpc.tag.equals(StorageControl.RPC_GetShard)){
                // requesting a specific shard file to be sent back to the caller
                String fileName = rpc.getMsg().split(StorageControl.PARAM_SEPARATOR)[0];
                Log.info(rpc.tag, fileName);

                // construct the shard from its full name
                FileShard fs = new FileShard(fileName);

                // send it to the caller
                connection.sendTCP(fs);
            }

            else if (rpc.tag.equals(StorageControl.RPC_RequestShardList)){
                // called to report back the list of Shards for a given filename
                String fileName = rpc.getMsg().split(StorageControl.PARAM_SEPARATOR)[0];

                StringBuffer shards = new StringBuffer("");

                if(StorageControl.getFileShardMap().containsKey(fileName)) {
                    for (Iterator<FileShard> it = StorageControl.getFileShardMap().get(fileName).iterator(); it.hasNext(); ) {
                        String shardName = it.next().toString();
                        shards.append(String.format("%s" + StorageControl.PARAM_SEPARATOR, shardName));
                    }
                    ArrayList<String> params = new ArrayList<String>();
                    params.add(shards.toString());

                    StorageControl.canProceed(false);
                    connection.sendTCP(StorageControl.getRPC(StorageControl.RPC_ListShards, params));
                }
            }

            else if (rpc.tag.equals(StorageControl.RPC_ReplicateShard)){
                String host = rpc.getMsg().split(StorageControl.PARAM_SEPARATOR)[0];
                String shard = rpc.getMsg().split(StorageControl.PARAM_SEPARATOR)[1];
                Log.info(rpc.tag.toString(), "Host: " + host);
                Log.info(rpc.tag.toString(), "Shard: " + shard);

                ArrayList<String> params = new ArrayList<String>();
                params.add(shard);
                Node.sendTCP(Node.getNodeByHostname(host), StorageControl.getRPC(StorageControl.RPC_GetShard,params));
            }

            else if (rpc.tag.equals(StorageControl.RPC_ValidateFile)){
                String fileName = rpc.getMsg().split(StorageControl.PARAM_SEPARATOR)[0];
                int fileSize = Integer.valueOf(rpc.getMsg().split(StorageControl.PARAM_SEPARATOR)[1]);
                int shardCount = Integer.valueOf(rpc.getMsg().split(StorageControl.PARAM_SEPARATOR)[2]);

                if (StorageControl.getFileShards(fileName) != null) {
                    if (StorageControl.getFileShards(fileName).size() == shardCount) {
                        int localFileSize = 0;
                        for (FileShard shard : StorageControl.getFileShards(fileName)) {
                            localFileSize += shard.getPayload().length;
                        }
                        if (localFileSize == fileSize) {
                            ArrayList<String> params = new ArrayList<String>();
                            params.add(fileName);
                            connection.sendTCP(StorageControl.getRPC(StorageControl.RPC_AckFile, params));
                            try{
                                String outFilePath = fileName.substring(0, fileName.indexOf("."))+"/"+fileName;
                                FileControl.mergeFiles(StorageControl.getFileShards(fileName), outFilePath);
                                StorageControl.initiateFileDistribution(outFilePath);
                            }catch (Exception e){
                                e.printStackTrace();
                            }
                        }
                    }
                }
            }

            // -------   Begin Crane RPC Checks   ----------
            else if (rpc.tag.equals(StorageControl.RPC_CraneWorkerInit)){
                Log.info("Crane-Init", String.format("Setting Coordinator to %s, and active utility to %s",
                        rpc.getMsg().split(StorageControl.PARAM_SEPARATOR)[0],
                        rpc.getMsg().split(StorageControl.PARAM_SEPARATOR)[1]));
                WorkerControl.setCoordinator(rpc.getMsg().split(StorageControl.PARAM_SEPARATOR)[0]);
                WorkerControl.init(rpc.getMsg().split(StorageControl.PARAM_SEPARATOR)[1]);
            }

            else if (rpc.tag.equals(StorageControl.RPC_CraneWorkerNextHop)){
                Log.info("Crane-NextHop", String.format("Setting NextHop to %s", rpc.getMsg().split(StorageControl.PARAM_SEPARATOR)[0]));
                WorkerControl.setNextHop(rpc.getMsg().split(StorageControl.PARAM_SEPARATOR)[0]);
            }

            else if (rpc.tag.equals(StorageControl.RPC_CraneEnd)){
                Log.info("Crane-End");
                if (WorkerControl.getGateKeeper().getUtil().isCollector()){
//                    Iterator it = WorkerControl.getApp().getResult().entrySet().iterator();
//                    while (it.hasNext()) {
//                        Log.info("Crane-Collection", it.next().toString());
//                    }
                    Log.info("yes", WorkerControl.getApp().getResult().get("yes").toString());
                    Log.info("wished", WorkerControl.getApp().getResult().get("wished").toString());
                    Log.info("wisdom", WorkerControl.getApp().getResult().get("wisdom").toString());
                    Log.info("vessels", WorkerControl.getApp().getResult().get("vessels").toString());
                    Log.info("spoke", WorkerControl.getApp().getResult().get("spoke").toString());
                }
            }
        }

        if (object instanceof FileShard){
            FileShard shard = (FileShard) object;
            String fileName = shard.getFileName() + FileShard.SHARD_PART_TAG + shard.getSeqNum();
            String partPath = shard.getShardPath() + File.separator + shard.getFileName().substring(0, shard.getFileName().indexOf(".")) + File.separator + FileShard.SHARD_PART_DIR;
            String filePath = partPath + File.separator + fileName;

            // create path to file parts
            new File(partPath).mkdirs();

            StorageControl.trackShard(shard.getFileName(), shard);

            Log.warn("Received", String.format("FileShard: %s", filePath));
            try {
                FileOutputStream bw = new FileOutputStream(filePath);
                bw.write(shard.getPayload());
                bw.flush();
                bw.close();

                ArrayList<String> params = new ArrayList<String>();
                params.add(shard.getFileName());
                connection.sendTCP(StorageControl.getRPC(StorageControl.RPC_AckShard, params));
            } catch (IOException ex) {ex.printStackTrace();}
        }

        if (!(object instanceof FrameworkMessage.KeepAlive) &&
                (!(object instanceof RPC) || (object instanceof RPC && !((RPC) object).tag.equals(StorageControl.RPC_KeepAlive)))) {
            Log.info("Transaction", String.format("Ending transaction with node: %s\n" +
                    "\t****************", connection.getRemoteAddressTCP().getHostName()));
            Node.endTransaction(connection.getRemoteAddressTCP().getHostName());
        }
    }

    @Override
    public void disconnected(Connection connection) {
        InetSocketAddress address = connection.getRemoteAddressTCP();
        if((address != null) && (PeerGroup.getInstance().isPeeredWith(address.getHostName()))){
            PeerGroup.getInstance().RemovePeer(address.getHostName());
            Log.warn("Disconnect", "------------> Client " + connection.getRemoteAddressUDP() + " [" + connection.getEndPoint() + "] disconnected <-----------");
            Log.warn("Disconnect", " Current Members:\n" + PeerGroup.getInstance().EnumeratePeers());
        }
    }
}
