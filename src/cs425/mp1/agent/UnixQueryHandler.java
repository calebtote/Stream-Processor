package cs425.mp1.agent;

import cs425.mp1.common.QueryProfile;
import cs425.mp1.common.QueryResults;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Created by Ahmed on 9/1/2015.
 */
public class UnixQueryHandler extends QueryHandler {
    public UnixQueryHandler() {
        super();
    }


    protected InputStream executeCommand(QueryProfile profile, String query) throws IOException {
        Runtime rt = Runtime.getRuntime();
        String[] cmd = {"/bin/sh", "-c", "grep " +
                            (profile.isIgnoreCase() ? " -i " : "") +
                            (profile.isRegExpression() ? " -G " : "") +
                            "'" + query + "' " +
                            profile.getFileName()};
        Process proc = rt.exec(cmd);
        return proc.getInputStream();
    }


}
