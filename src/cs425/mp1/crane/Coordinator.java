package cs425.mp1.crane;

import com.esotericsoftware.minlog.Log;

import cs425.mp1.crane.apps.AppBase;
import cs425.mp1.crane.apps.TestApp;
import cs425.mp1.crane.utils.BaseUtil;
import cs425.mp1.network.Node;
import cs425.mp1.network.RPC;
import cs425.mp1.network.StorageControl;
import cs425.mp1.network.StorageTopology;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

/**
 * Created by ctote on 12/2/15.
 */
public class Coordinator {
    public Coordinator(AppBase app){
        this.app = app;
        this.collectorNodes = new HashSet<>();
    }
    private static AppBase app;
    protected static Set<String> collectorNodes;

    // { hostname : utility assigned}
    private static Map<String, BaseUtil> activeCraneWorkers = new HashMap<>();
    public static void prepareTopology(){
        WorkerControl.setCoordinator(Node.getLocal().getHostname());
        for (String host : StorageTopology.GetStorageTopology().values()){
            if (app.getUtilities().peek() == null)
                break;
            if (host.equals(WorkerControl.getCoordinator().getHostname()))
                continue;

            System.out.println(host);
            System.out.println(WorkerControl.getCoordinator());
            Log.info("Coord", String.format("Putting %s in map with Utility %s", host, app.getUtilities().peek()));
            if (app.getUtilities().peek().isCollector())
                collectorNodes.add(host);

            activeCraneWorkers.put(host, app.getUtilities().pop());
        }
    }

    public static void launchTopology(){
        Iterator it = activeCraneWorkers.keySet().iterator();
        System.out.println("Active Crane Workers: " + activeCraneWorkers.keySet());
        it.next();

        for (String host : activeCraneWorkers.keySet()){
            System.out.println("Current Worker: " + host);

            Node.sendTCP(Node.getNodeByHostname(host), app);

            ArrayList<String> params = new ArrayList<>();
            params.add(Node.getLocal().getHostname());
            params.add(activeCraneWorkers.get(host).getName());
            System.out.println("Params: " + params);
            Node.sendTCP(Node.getNodeByHostname(host), StorageControl.getRPC(StorageControl.RPC_CraneWorkerInit, params));

            ArrayList<String> params2 = new ArrayList<>();
            if (it.hasNext())
                params2.add(it.next().toString());
            else
                params2.add(WorkerControl.getCoordinator().getHostname());
            System.out.println("Params2: " + params2);

            RPC rpc = StorageControl.getRPC(StorageControl.RPC_CraneWorkerNextHop, params2);
            Node.sendTCP(Node.getNodeByHostname(host), rpc);
        }
    }

    public static void Stream(){
        try (BufferedReader br = new BufferedReader(new FileReader(app.getDataPath()))) {
            String line;
            StreamWrapper sw = new StreamWrapper();
            Integer maxWorkLoad = 500;
            Integer unitsOfWork = 0;
            String initHost = activeCraneWorkers.keySet().iterator().next();
            while ((line = br.readLine()) != null) {
                sw.pushWork(line);
                unitsOfWork++;

                // Start stream at front of worker set
                if (unitsOfWork >= maxWorkLoad) {
                    Node.sendTCP(Node.getNodeByHostname(initHost), sw);
                    unitsOfWork = 0;
                 //   sw.getWorkingDeque().clear();
                }
            }

            // Make sure the last batch is sent
            if (sw.peekWork() != null) {
                Node.sendTCP(Node.getNodeByHostname(initHost), sw);
            }

            Thread.sleep(5000);
            for (String host : collectorNodes) {
                Log.info("CraneEnd", host);
                Node.sendTCP(Node.getNodeByHostname(host), StorageControl.getRPC(StorageControl.RPC_CraneEnd, new ArrayList<String>()));
            }

        } catch (Exception e){
            e.printStackTrace();
        }
    }
}
