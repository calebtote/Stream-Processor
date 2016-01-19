package cs425.mp1.network;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * A Peer Connection is a Node entry in the list of peers in a PeerGroup.
 * It is a data record containing:
 * 1. The instance it was created
 * 2. The node referred to
 * 3.
 */
public class PeerConnection {

    public Heartbeat heartbeat = null;
    //map of heartbeat objects per Peer connection
    public static List<Heartbeat> Heartbeats = new ArrayList<Heartbeat>();

    public Heartbeat GenerateHeartbeat(){
        Heartbeat hb = new Heartbeat();
        Heartbeats.add(hb);
        return hb;
    }

    //Active heartbeat is always the end of the list
    public Heartbeat  GetActiveHeartbeat(){
        return Heartbeats.get(Heartbeats.size() - 1);
    }

    public void SetActiveHeartbeat(Heartbeat hb){
        Heartbeats.remove(Heartbeats.get(Heartbeats.size() - 1));
        Heartbeats.add(hb);
    }

    private DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss.SSS");
    private Calendar cal = Calendar.getInstance();

//    private InetSocketAddress udpAddr = null;

    public Node getPeer() {
        return peer;
    }

    private Node peer = null;
    private Date joinTime = null;
    private Date lastUpdate = null;

    private short seqNum = 0;

    public InetSocketAddress getUdpAddr(){ return peer.getClient().getConnection().getRemoteAddressUDP(); }
    public String getPeerHostname(){ return peer.getHostname(); }
    public Date getPeerJoinTime(){ return joinTime; }
    public Date getPeerLastUpdate(){ return lastUpdate; }
    public void setPeerLastUpdate(Date _newUpdate) { lastUpdate = _newUpdate;}
    public long getPeerUpdateAge(){ Date now = new Date(); return now.getTime() - lastUpdate.getTime(); }
    public void updateSeqNum(Short num){ if (this.seqNum < num) { this.seqNum = num; } }

    public PeerConnection() {
        this.joinTime = new Date();
        this.lastUpdate = this.joinTime;
        this.heartbeat = new Heartbeat();
    }

    public PeerConnection(Node _peer){
        this.peer = _peer;
        this.joinTime = new Date();
        this.lastUpdate = this.joinTime;
        this.heartbeat = new Heartbeat();
    }
    public void UpdateLastCheckDate(){
        this.lastUpdate = new Date();
    }
    public String getID() {
        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss.SSS");
        return  "["+dateFormat.format(getPeerLastUpdate()) + "]\t" + getPeerHostname()+"\t";}

    public boolean isItMe(){
        boolean itIsMe = false;
        try {
            itIsMe =  (getPeerHostname().equals(InetAddress.getLocalHost().getHostName()));
        } catch (UnknownHostException e) { e.printStackTrace();}
        return itIsMe;
    }

}
