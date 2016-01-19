package cs425.mp1.network;

import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.FrameworkMessage;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.minlog.Log;
import cs425.mp1.common.FileShard;
import cs425.mp1.common.QueryResults;
import cs425.mp1.crane.WorkerControl;
import cs425.mp1.crane.StreamWrapper;
import cs425.mp1.main.ConfigManager;

import java.io.*;
import java.util.ArrayList;
import java.util.Set;
import java.util.concurrent.Future;

/**
 */
public class MasterListener extends Listener {
    private File outputFile = null;

    public MasterListener() {
        try {
            outputFile = new File(ConfigManager.getProperties().getProperty("outputFile"));
            if (outputFile.exists()) {
                new FileOutputStream(outputFile, false); // wipe out the contents
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void received(Connection connection, Object object) {
        if (!(object instanceof FrameworkMessage.KeepAlive) &&
                (!(object instanceof RPC) || (object instanceof RPC && !((RPC) object).tag.equals(StorageControl.RPC_KeepAlive)))) {
            Log.info("Transaction-m", String.format("Starting transaction with Node: %s", connection.getRemoteAddressTCP().getHostName()));
            Log.info("Transaction-m", String.format("Object: %s", object.getClass().getSimpleName()));
            Node.startTransaction(connection.getRemoteAddressTCP().getHostName());
        }

        if (object instanceof StreamWrapper) {
            StreamWrapper sw = (StreamWrapper) object;
           // Set<Future> f = WorkerControl.getGateKeeper().EnqueueWork(sw);
        }

        if (object instanceof QueryResults) {
            QueryResults response = (QueryResults) object;
            try {
                PrintWriter writer = new PrintWriter(new FileOutputStream(outputFile, true));
                output("**********************************************", System.out, writer);
                output("Results from Sender: " + connection.getRemoteAddressTCP(), System.out, writer);
                output("Found " + response.getResultsCount() + " results in " + response.getExecutionTime() + " micro sec", System.out, writer);
                writer.print(response.getResults());
                output("**********************************************", System.out, writer);
                writer.flush();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        else if (object instanceof Heartbeat) {
            Heartbeat hb = (Heartbeat) object;
            if (hb.msg.equals(hb.beat)){
                hb.msg = hb.ack;
                Log.trace("Heartbeat", String.format("Got beat from %s, sending ack.", connection.getRemoteAddressUDP().getHostName()));
                connection.sendTCP(hb);
            }
            else{
                Log.trace("Heartbeat", String.format("Got ack from %s.", connection.getRemoteAddressUDP().getHostName()));
            }
        }

        else if (object instanceof RPC){
            RPC rpc = (RPC) object;
            if (!rpc.tag.equals(StorageControl.RPC_KeepAlive)) {
                Log.info("RPC-m", rpc.tag);
                Log.info("RPC-m", rpc.getMsg());
            }

            if (rpc.tag.equals(StorageControl.RPC_AckShard)){
                StorageControl.youCanProceed();
            }
            if (rpc.tag.equals(StorageControl.RPC_AckFile)){
                // do nothing for now
            }
            if (rpc.tag.equals(StorageControl.RPC_StorageView)){
                String[] fileList = rpc.getMsg().split(StorageControl.PARAM_SEPARATOR);
                for (String file : fileList){
                    Log.info("ListFiles", String.format("%s: %s", connection.getRemoteAddressTCP().getHostName(), file));
                }
            }
            if (rpc.tag.equals(StorageControl.RPC_ListShards)){
                StorageControl.setReturnObject(rpc.getMsg());
                StorageControl.youCanProceed();
                String[] shardList = rpc.getMsg().split(StorageControl.PARAM_SEPARATOR);
                Log.warn("ShardList", String.format("%s:", connection.getRemoteAddressTCP().getHostName()));
                for (String shard : shardList){
                    Log.warn("ShardList", String.format("\t%s", shard.substring(shard.lastIndexOf("/"), shard.length())));
                }
            }
        }

        else if (object instanceof FileShard){
            FileShard shard = (FileShard) object;
            String fileName = shard.getFileName() + FileShard.SHARD_PART_TAG + shard.getSeqNum();
            String partPath = shard.getShardPath() + File.separator + shard.getFileName().substring(0, shard.getFileName().indexOf(".")) + File.separator + FileShard.SHARD_PART_DIR;
            String filePath = shard.getShardPath() + File.separator + fileName;

            // create path to file parts
            new File(partPath).mkdirs();

            StorageControl.trackShard(shard.getFileName(), shard);
            System.out.println(shard.getFileName());
            System.out.println(filePath);

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

            StorageControl.setReturnObject(object);
            StorageControl.youCanProceed();
        }

        if (!(object instanceof FrameworkMessage.KeepAlive) &&
                (!(object instanceof RPC) || (object instanceof RPC && !((RPC) object).tag.equals(StorageControl.RPC_KeepAlive)))) {
            Log.info("Transaction-m", String.format("Ending transaction with node: %s\n" +
                    "\t****************", connection.getRemoteAddressTCP().getHostName()));
            Node.endTransaction(connection.getRemoteAddressTCP().getHostName());
        }
    }

    private void output(final String msg, PrintStream out1, PrintWriter out2) {
        out1.println(msg);
        out2.println(msg);
    }
}
