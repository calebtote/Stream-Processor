package cs425.mp1.network;

import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.minlog.Log;
import cs425.mp1.main.ConfigManager;

import java.io.IOException;
import java.net.ConnectException;

/**
 * Purpose:  Controls client communication to server hosts
 */
public class ClientControl {
    private Client connection = new Client(Integer.parseInt(ConfigManager.getProperties().getProperty("bufferSize")),
                                    Integer.parseInt(ConfigManager.getProperties().getProperty("bufferSize")));
    private MasterListener listener = null;
    public Client getConnection() {
        return connection;
    }

    public void Start() {

        if(connection.isConnected()) return;    // if already connected, no need to construct a new client

        this.connection.addListener(getListener());
        this.connection.start();
    }

    public MasterListener getListener() {
        // Make sure we have only one
        if (listener == null) listener = new MasterListener();
        return listener;
    }

    public Integer SendTCP(Node node, Object data) {
        int bytesSent = 0;

        if(!Node.getLocal().getHostname().equals(node.getHostname())) {
            try {
                this.connection.connect(Integer.parseInt(ConfigManager.getProperties().getProperty("connectionTimeout")),
                        node.getIP(), node.getServer().getTcp(), node.getServer().getUdp());
                if (data instanceof RPC) Log.debug("Send", "Sending on TCP " + ((RPC)data).getMsg() + " to " + node.getHostname());
                else                     Log.debug("Send", "Sending on TCP " + data.toString() + " to " + node.getHostname());

                bytesSent = this.connection.sendTCP(data);


            } catch (ConnectException e) {
                Log.warn(e.toString());
            } catch (IOException e){
                Log.warn(e.toString());
                return -1;
            }
        } //else Log.warn("Send", "Cannot send on TCP " + data.toString() + " to myslef " + node.getHostname());
        return bytesSent;
    }

    public Integer SendUDP(Node node, Object data) {
        int bytesSent = 0;
        if(!Node.getLocal().getHostname().equals(node.getHostname())) {
            try {
                this.connection.connect(Integer.parseInt(ConfigManager.getProperties().getProperty("connectionTimeout")),
                        node.getIP(), node.getServer().getTcp(), node.getServer().getUdp());
                //return this.connection.sendTCP(data);
                Log.debug("Send", "Sending on UDP " + data.toString() + " to " + node.getHostname());
                bytesSent = this.connection.sendUDP(data);
            } catch (ConnectException e) {
                Log.info(e.toString());
            } catch (IOException e) {
                Log.debug(e.toString());
            }
        }  //else Log.warn("Send", "Cannot send on UDP " + data.toString() + " to myslef " + node.getHostname());
        return bytesSent;
    }

    public void disconnectSession() {
        if(connection.isConnected())
            connection.stop();
    }

    public void reconnectSession() {
        if(connection.isConnected())
            try {
                connection.reconnect();
            } catch (IOException ex) {
                Start();
            }
    }
}
