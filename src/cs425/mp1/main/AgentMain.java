package cs425.mp1.main;

import cs425.mp1.agent.QueryHandler;
import cs425.mp1.common.QueryProfile;

/**
 */
public class AgentMain {
    public static void  main(String[] args) {
        String inputFile = "samples/sample.log";
        String query = "200";
        if(args.length > 0)
            inputFile = args[0];

        if(args.length > 1)
            query = args[1];

        if(args.length == 0){
            System.out.println("Usage: java -jar cs425.mp1.main.AgentMain [path to file] [what to grep]");
            System.out.println("Using defaults ...");
        }

        try {
            QueryProfile profile = new QueryProfile();
            profile.setFileName(inputFile);
            QueryHandler grep = QueryHandler.getQueryHandler();
            grep.handleQuery(profile, query);
            System.out.println("Searching " + inputFile + " for " + query);

            System.out.println("Found "+grep.getQueryResults().getResultsCount()+" results in "+grep.getQueryResults().getExecutionTime() +" ms");
            System.out.println(grep.getQueryResults().getResults());

        }catch (Exception ex) { ex.printStackTrace();}

    }
}
