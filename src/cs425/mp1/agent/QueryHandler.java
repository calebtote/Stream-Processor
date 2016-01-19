package cs425.mp1.agent;

import cs425.mp1.common.QueryException;
import cs425.mp1.common.QueryProfile;
import cs425.mp1.common.QueryResults;

import javax.management.Query;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Class to handle executing grep on the local machine based on a Query Profile
 */
public abstract class QueryHandler {
    //protected QueryProfile profile  = null;
    protected QueryResults results = null;
    private static String OS = System.getProperty("os.name").toLowerCase();

    protected abstract InputStream executeCommand(QueryProfile _profile, String query) throws IOException;
    public static boolean isWindows() { return (OS.indexOf("win") >= 0); }
    public static boolean isMac() { return (OS.indexOf("mac") >= 0); }
    public static boolean isUnix() { return (OS.indexOf("nix") >= 0 || OS.indexOf("nux") >= 0 || OS.indexOf("aix") > 0 ); }
    public static boolean isSolaris() { return (OS.indexOf("sunos") >= 0); }

    public QueryResults getQueryResults() {
        return results;
    }

    public void handleQuery(QueryProfile _profile, String query) throws IOException {
        results = new QueryResults();

        long startTime = System.nanoTime();
        long endTime = System.nanoTime();
        InputStream iStream = executeCommand(_profile, query);
        results.setExecutionTime((endTime - startTime) / (double)1000);  // in ms

        BufferedReader is = new BufferedReader(new InputStreamReader(iStream));
        String line;
        while ((line = is.readLine()) != null) {
            results.addResultLine(line);
        }
    }

    public static QueryHandler getQueryHandler() throws QueryException {
        QueryHandler handler = null;

        if (isWindows()) return handler = new WindowsQueryHandler();
        if (isUnix()) return handler = new UnixQueryHandler();
        if (isMac()) return handler = new MacQueryHandler();
        if (isSolaris()) throw new QueryException("Operating System not supported");
        return handler;
    }
}
