package cs425.mp1.agent;

import cs425.mp1.common.QueryProfile;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created by Ahmed on 9/1/2015.
 */
public class MacQueryHandler extends QueryHandler {
    public MacQueryHandler() { super();}



    protected InputStream executeCommand(QueryProfile profile, String query) throws IOException {
        Runtime rt = Runtime.getRuntime();
        String[] cmd = { "/bin/sh", "-c", "grep '"+query+"' "+profile.getFileName() };
        Process proc = rt.exec(cmd);
        return proc.getInputStream();
    }



}
