package cs425.mp1.main;

import com.esotericsoftware.minlog.Log;
import cs425.mp1.common.FileShard;
import cs425.mp1.common.QueryException;
import cs425.mp1.crane.Coordinator;
import cs425.mp1.crane.WorkerControl;
import cs425.mp1.crane.apps.TestApp;
import cs425.mp1.network.*;

import java.io.File;
import java.util.*;



//TODO: Here's the basic flow I had in mind:
//TODO:     - We maintain a list of peers [Control.PeerGroup]
//TODO:     - If we receive a disconnect, we remove the peer from this group
//TODO:     - We periodically check our known nodes [Node.NodeMap] for new "alive" connections
//TODO:     - We skip nodes that are in our current [Control.PeerGroup]
//TODO:     - As a backup (i.e., we missed the TCP disconnect), we pass [PeerLists] (not implemented yet)
//TODO:     -   -   If we recognize a new host, send a heartbeat to verify
//TODO:     -   -   If the [PeerList] is missing one of our peers, send a heartbeat to verify
//TODO:     -   -   Update our local [PeerList] based on above results
//TODO:     -   We need to come up with a reasonable time frame to send PeerLists
//TODO:     - I noticed the framework is sending com.esotericsoftware.kryonet.FrameworkMessage$KeepAlive objects to our current peers - we can probably leverage this

//TODO:     - Note: Local runs use the config file in /out/production; Deployed hosts use the one in /out/artifacts

/**
 */
public class UX {

    private static Set<String> HostNames = new HashSet<String>();

    // Start the Timer Thread as a Daemon
    public static Timer timer = new Timer("TryIdlePeers", true);
    public static void PauseTimerTask(){ timer.cancel(); }
    public static void StartTimerTask(){
        // TimerTask --> Try to make new peers, check already connected ones
        // Repeat this task after initial delay of 0, 10 secs apart
        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                if (Node.isRespondingMode()) {
                    Log.trace("[MP-CS425] --------------  Timer Task Clicked --------------");
                    // Check if the list of nodes are all peered or not
                    PeerGroup.getInstance().PeerWithNodes();
                    // This checks idle peers every 10 seconds (see timer below)
                    PeerGroup.getInstance().CheckPeers();
                }
                System.gc();
            }
        };

        // Repeat this task after initial delay of 0, x secs apart --> Change values from the configuration properties
        timer = new Timer("TryIdlePeers", true);
        timer.scheduleAtFixedRate(timerTask, Integer.parseInt(ConfigManager.getProperties().getProperty("PeerCheckAfter")),
                Integer.parseInt(ConfigManager.getProperties().getProperty("PeerCheckEvery")));
    }

    public static void main(String[] args) {
        //Log.set(Log.LEVEL_DEBUG);
        Log.set(Integer.parseInt(ConfigManager.getProperties().getProperty("logLevel")));
        try {
            InitializeHostnames();
            ConstructNodes();
            StorageControl storage = new StorageControl();

//                String query = getInput();
//
//                QueryProfile profile = new QueryProfile();
//                String inputFile = ConfigManager.getProperties().getProperty("inputFile");
//                profile.setFileName(inputFile);
//                profile.setIsRegExpression(Boolean.parseBoolean(ConfigManager.getProperties().getProperty("isRegularExpression")));
//                profile.setIgnoreCase(Boolean.parseBoolean(ConfigManager.getProperties().getProperty("ignoreCase")));
//                profile.setQuery(query);


            StartTimerTask();
            boolean quit = false;

            while (!quit) {
                try {
                    String command = getInput().toLowerCase();
                    String[] tokenizedCommand = command.split(" ");

                    if (tokenizedCommand[0].equals("stream")){
                        Log.info("Streaming!");

                        TestApp a = new TestApp();
                        a.setDataPath(tokenizedCommand[1]);
                        a.setUtilities();

                        Coordinator c = new Coordinator(a);
                        c.prepareTopology();
                        c.launchTopology();
                        c.Stream();
                    }

                    if (tokenizedCommand[0].equals("put")) {  // put file
                        if (tokenizedCommand.length > 1) {

                            System.out.println("[MP-CS425] --  putting " + tokenizedCommand[1]);
                            if (StorageControl.getMaster().equals(Node.getLocal().getHostname())) { // I am the master
                                Log.warn("MainCommand", "Initiating File Send from the master");
                                StorageControl.initiateFileDistribution(tokenizedCommand[1]);
                            } else {
                                PauseTimerTask();
                                List<FileShard> fileShards = StorageControl.uploadFileToMaster(tokenizedCommand[1]);
                                StartTimerTask();

                                Thread.sleep(1000);
                                ArrayList<String> params = new ArrayList<String>();
                                params.add(tokenizedCommand[1]);
                                params.add(String.valueOf(new File(tokenizedCommand[1]).length()));
                                params.add(String.valueOf(fileShards.size()));
                                StorageControl.sendRPC(StorageControl.getMaster(), StorageControl.RPC_ValidateFile, params);
                            }
                        } else System.out.println ("put Command should have the name of the file");
                    }

                    if (tokenizedCommand[0].equals("get")) {   // merge file
                        if (tokenizedCommand.length > 2) {

                        // 1. get the merge original filename and the new filename to output
                            String orignalFileName =  tokenizedCommand[1];
                            String newFileName =  tokenizedCommand[2];
                            StorageControl.mergeFileFromShards(orignalFileName, newFileName);

                        } else System.out.println ("get Command should have the name of the file and the target new file name");
                    }
                    if (tokenizedCommand[0].equals("del")) {   // merge file
                        if (tokenizedCommand.length > 1) {

                            // 1. get the merge original filename and the new filename to output
                            String orignalFileName =  tokenizedCommand[1];
                            String newFileName =  tokenizedCommand[2];
                            StorageControl.mergeFileFromShards(orignalFileName, newFileName);

                        } else System.out.println ("del Command should have the name of the file ");
                    }
                    if (tokenizedCommand[0].equals("list")) {
                        for (String host : StorageTopology.GetStorageTopology().values()){
                            if (host.equals(Node.getLocal().getHostname()))
                                continue;

                            ArrayList<String> params = new ArrayList<String>();
                            params.add(Node.getLocal().getHostname());
                            StorageControl.sendRPC(host, StorageControl.RPC_RequestStorageView, params);
                            Thread.sleep(500);
                        }
                    }
                    if (tokenizedCommand[0].equals("listsh")) {   // verbose
                        for (String host : StorageTopology.GetStorageTopology().values()){
                            //if (host.equals(Node.getLocal().getHostname()))
                              //  continue;

                            ArrayList<String> params = new ArrayList<String>();
                            params.add(tokenizedCommand[1]);
                            StorageControl.sendRPC(host, StorageControl.RPC_RequestShardList, params);
                            Thread.sleep(1000);
                        }
                    }
                    if (tokenizedCommand[0].equals("store")) {   // verbose
                        for (String host : StorageTopology.GetStorageTopology().values()){
                            //if (host.equals(Node.getLocal().getHostname()))
                            //  continue;

                            ArrayList<String> params = new ArrayList<String>();
                            //params.add(tokenizedCommand[1]);
                            StorageControl.sendRPC(host, StorageControl.RPC_RequestShardList, params);
                            Thread.sleep(1000);
                        }
                    }
                    if (command.equals("l")) {  // list peers
                        if (Node.isRespondingMode())
                            System.out.println("[MP-CS425] -- Current List of Members\n" + PeerGroup.getInstance().EnumeratePeers());
                        else System.out.println("[MP-CS425] -- Cannot list members while in disconnected mode");

                    }
                    if (command.startsWith("k")) {   // appear offline
                        if (!Node.isRespondingMode()) {
                            System.out.println("[MP-CS425] -- Already in Offline Mode. Use command \"A\" or \"a\" to get online");
                        } else {
                            System.out.println("[MP-CS425] -- Going to Offline Mode.");
                            Node.setRespondingMode(false);
                            Node.getLocal().disconnect();
                        }
                    }

                    if (command.startsWith("a")) { // alive
                        if (Node.isRespondingMode()) {
                            System.out.println("[MP-CS425] -- Already in Online Mode. Use command \"K\" or \"k\" to get offline");
                        } else {
                            System.out.println("[MP-CS425] -- Going to Online Mode.");
                            Node.setRespondingMode(true);
                            Node.getLocal().reconnect();
                        }
                    }

                    if (command.startsWith("i")) { // id
                        if (Node.isRespondingMode())
                            System.out.println("[MP-CS425] -- Node ID " + Node.getLocal().getHostname());
                        else System.out.println("[MP-CS425] -- Cannot evaluate ID while in disconnected mode");
                    }

                    if (command.startsWith("q")) {  // quit
                        quit = true;
                    }
                } catch (Exception ex) {ex.printStackTrace();}
            }
            System.exit(0);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static String getInput() {
        String result = null;
        while (result == null || result.trim().isEmpty()) {
            Scanner reader = new Scanner(System.in);
            System.out.println((result == null) ? "Enter Command:  " : "Invalid command.. try again [A(live), L(ist Members), K(ill), I[d], Q(uit)] ..");
            result = reader.nextLine();
        }
        return result;
    }

    public static void InitializeHostnames() throws QueryException {
        String hostsProperty = ConfigManager.getProperties().getProperty("hosts");
        if (hostsProperty == null || hostsProperty.trim().isEmpty())
            throw new QueryException("Hosts are not in Configuration file");
        String[] hosts = hostsProperty.split(",");
        for (int i = 0; i < hosts.length; i++) {
            HostNames.add(hosts[i]);
        }
    }

    public static void ConstructNodes() {
        Node genNode = null;
        for (String hostname : HostNames) {
            genNode = new Node(hostname);
            //System.out.println(genNode.getIP());
        }
    }
}
