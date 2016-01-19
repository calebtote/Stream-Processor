package cs425.mp1.network;

import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Server;
import com.esotericsoftware.minlog.Log;
import cs425.mp1.common.FileShard;
import cs425.mp1.common.QueryProfile;
import cs425.mp1.common.QueryResults;
import cs425.mp1.crane.StreamWrapper;
import cs425.mp1.crane.apps.AppBase;
import cs425.mp1.crane.apps.TestApp;
import cs425.mp1.crane.utils.BaseUtil;
import cs425.mp1.crane.utils.RemovePunctuationUtil;
import cs425.mp1.crane.utils.ToLowercaseUtil;
import cs425.mp1.crane.utils.WordCountUtil;
import cs425.mp1.main.ConfigManager;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * Purpose: Maintains details and communications for the node, including both server and client connections.
 */
public class Node {

    // Getters & Setters
    // ---------------------
    public static HashMap getNodeMap() {
        return NodeMap;
    }
    public ClientControl getClient() {
        return client;
    }
    public ServerControl getServer() {
        return server;
    }
    public String getHostname() { return hostname; }
    public java.net.InetAddress getIP() { return ipv4; }
    // -----------------------
    // End Getters & Setters

    private static HashMap NodeMap = new HashMap<String, Node>();
    private ServerControl server = null;
    private ClientControl client = null;
    private String hostname = null;
    private java.net.InetAddress ipv4 = null;

    public static boolean isRespondingMode() {
        return respondingMode;
    }

    public static void setRespondingMode(boolean _respondingMode) {
        respondingMode = _respondingMode;
    }

    private static boolean respondingMode = true;

    private static Set<String> activeTransactions = new HashSet<String>();
    public static Set<String> getActiveTransactions() { return activeTransactions; }
    public static void startTransaction (String host) { activeTransactions.add(host); }
    public static void endTransaction(String host) { if (activeTransactions.contains(host)) activeTransactions.remove(host); }
    public static boolean inTransactionWith(String host) {
        if (activeTransactions.contains(host))
            return true;
        return false;
    }

    public Node() {
        this(getLocal().getHostname());
    }

    // hostname:    The hostname of the box
    // ipv4:        The IPV4 address of the box
    public Node(String hostname) {
        this.hostname = hostname;
        NodeMap.put(this.hostname, this);

        try {
            this.ipv4 = InetAddress.getByName(this.hostname);
        } catch (UnknownHostException e) {
            Log.ERROR();
            Log.error(e.toString());
            //System.exit(2);
        }

        server = new ServerControl(Integer.parseInt(ConfigManager.getProperties().getProperty("serverTCPPort")),
                                    Integer.parseInt(ConfigManager.getProperties().getProperty("serverUDPPort")));
        client = new ClientControl();

        try {
            if (this.hostname.equals(InetAddress.getLocalHost().getHostName()))
                StartServer();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }

        Registrar.RegisterClass(this, QueryProfile.class);
        Registrar.RegisterClass(this, QueryResults.class);
        Registrar.RegisterClass(this, HashSet.class);
        Registrar.RegisterClass(this, ArrayDeque.class);
        Registrar.RegisterClass(this, ConcurrentLinkedDeque.class);
        Registrar.RegisterClass(this, TreeMap.class);

        Registrar.RegisterClass(this, PeerGroup.class);
        Registrar.RegisterClass(this, Node.class);
        Registrar.RegisterClass(this, ClientControl.class);
        Registrar.RegisterClass(this, ServerControl.class);
        Registrar.RegisterClass(this, InetAddress.class);
        Registrar.RegisterClass(this, Client.class);
        Registrar.RegisterClass(this, Integer.class);
        Registrar.RegisterClass(this, Server.class);
        Registrar.RegisterClass(this, MasterListener.class);
        Registrar.RegisterClass(this, AgentListener.class);
        Registrar.RegisterClass(this, File.class);
        Registrar.RegisterClass(this, StorageControl.class);
        Registrar.RegisterClass(this, FileShard.class);
        Registrar.RegisterClass(this, byte[].class);
        Registrar.RegisterClass(this, RPC.class);

        Registrar.RegisterClass(this, PeerConnection.class);
        Registrar.RegisterClass(this, Heartbeat.class);
        Registrar.RegisterClass(this, java.util.HashMap.class);
        Registrar.RegisterClass(this, DateFormat.class);
        Registrar.RegisterClass(this, SimpleDateFormat.class);
        Registrar.RegisterClass(this, java.text.DateFormatSymbols.class);
        Registrar.RegisterClass(this, String[].class);
        Registrar.RegisterClass(this, Calendar.class);

        Registrar.RegisterClass(this, Locale.class);
        Registrar.RegisterClass(this, Date.class);
        Registrar.RegisterClass(this, GregorianCalendar.class);

        // Crane Base
        Registrar.RegisterClass(this, StreamWrapper.class);

        // Crane Apps
        Registrar.RegisterClass(this, AppBase.class);
        Registrar.RegisterClass(this, TestApp.class);

        // Crane Utilities
        Registrar.RegisterClass(this, BaseUtil.class);
        Registrar.RegisterClass(this, ToLowercaseUtil.class);
        Registrar.RegisterClass(this, WordCountUtil.class);
        Registrar.RegisterClass(this, RemovePunctuationUtil.class);
    }

    private void StartServer(){
        this.server.Start();
    }


    public boolean isItMe(){
        boolean itIsMe = false;
        try {
            itIsMe =  (getHostname().equals(InetAddress.getLocalHost().getHostName()));
        } catch (UnknownHostException e) { e.printStackTrace();}
        return itIsMe;
    }
    // Gets local node so we can send messages out via it's connection object
    public static Node getLocal(){
        try {
            return getNodeByHostname((InetAddress.getLocalHost().getHostName()));
            }catch (UnknownHostException e) {
                e.printStackTrace();
            }
        return null;
    }

    /** sends UDP directly from the local host
     *
     * @param node and data
     * @return
     */
    public static Integer sendUDP(Node node, Object data) {
        return getLocal().getClient().SendUDP(node, data);
    }

    /** sends TCP directly from the local host
     *
     * @param node and data
     * @return
     */
    public static Integer sendTCP(Node node, Object data) {
        return getLocal().getClient().SendTCP(node, data);
    }


    public static Node getNodeByHostname (String hostname) {
        Node node= null;
        Map<String, Node> nodeMap = Node.getNodeMap();
        for (Map.Entry<String, Node> entry : nodeMap.entrySet()) {

            if (entry.getValue().getHostname().startsWith(hostname))
                node = entry.getValue();
        }
        if(node == null) Log.error("Could not find node in the node map "+hostname);
        return node;
    }

    public static Node getCorrespondingNode(Connection connection) {
        return getNodeByHostname( connection.getRemoteAddressUDP().getHostName());
    }

    public void disconnect() {
        // disconnect clients
        getClient().disconnectSession();
        // disconnect servers
        getServer().disconnect();
    }

    public void reconnect() {
        // reconnect clients
        getClient().reconnectSession();
        // reconnect servers
        getServer().reconnect();
    }
}
