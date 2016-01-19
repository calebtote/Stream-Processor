package cs425.mp1.crane;

import cs425.mp1.crane.apps.AppBase;
import cs425.mp1.network.Node;

/**
 * Created by ctote on 12/4/15.
 */
public class WorkerControl {
    public static void init(String utilName){
        activeUtility = utilName;
        gk = new GateKeeper(WorkerControl.app.utilNameMap.get(utilName));
    }

    public static StreamWrapper prepareForTransport(StreamWrapper sw){
        StreamWrapper tmp = new StreamWrapper();
        tmp.setWorkingDeque(sw.getCompletionDeque());
        return tmp;
    }

    private static AppBase app;
    public static void setApp(AppBase a){ app = a; }
    public static AppBase getApp(){ return app; }

    private static Node coordinator = null;
    public static void setCoordinator(String hostname){ coordinator = Node.getNodeByHostname(hostname); }
    public static Node getCoordinator(){ return coordinator; }

    private static Node nextHop = null;
    public static void setNextHop(String host){ nextHop = Node.getNodeByHostname(host); }
    public static Node getNextHop(){ return nextHop; }

    private static String activeUtility;
    public String getActiveUtility(){ return activeUtility; }

    private static GateKeeper gk;
    public static GateKeeper getGateKeeper(){ return gk; }

}