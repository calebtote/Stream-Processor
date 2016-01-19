package cs425.mp1.network;

import com.esotericsoftware.kryonet.Server;
import com.esotericsoftware.minlog.Log;
import cs425.mp1.main.ConfigManager;

import java.io.IOException;

import static java.lang.System.out;

/**
 * Purpose:  Controls server handling of incoming client connections
 */
public class ServerControl {

    public Integer getTcp() {
        return tcp;
    }
    public Integer getUdp() { return udp; }
    public Server getConnection() {
        return connection;
    }

    public ServerControl(Integer tcpPort, Integer udpPort) {
        this.tcp = tcpPort;
        this.udp = udpPort;
        this.connection = new Server(Integer.parseInt(ConfigManager.getProperties().getProperty("bufferSize")),
                Integer.parseInt(ConfigManager.getProperties().getProperty("bufferSize")));
    }

    private Server connection = null;
    private Integer tcp = null;
    private Integer udp = null;

    public void Start() {
        Log.info("Initializing listener on tcp:" + this.tcp + "/udp:" + this.udp);

        try {
            this.connection.start();
            this.connection.bind(this.tcp, this.udp);
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        //TODO: Update this listener to properly deconstruct the incoming query data
        this.connection.addListener(new AgentListener());
    }

    public void disconnect() {
        connection.stop();
    }

    public void reconnect() {
        Start();
    }
}
