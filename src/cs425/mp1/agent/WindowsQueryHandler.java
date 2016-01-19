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
public class WindowsQueryHandler extends QueryHandler{
    public WindowsQueryHandler() { super();}

    protected InputStream executeCommand3(QueryProfile profile,String query) throws IOException {
        ProcessBuilder pb = new ProcessBuilder(
                "d:/Tools/grep/bin/grep.exe",
                " -G ",
                " \""+query+"\" ",
                profile.getFileName());
        pb.redirectErrorStream(true);
        Process proc = pb.start();
        try {
            int errCode = proc.waitFor();
            System.out.println("grep command executed, any errors? " + (errCode == 0 ? "No" : "Yes "+errCode));
        }catch (Exception e) {e.printStackTrace();}
        return proc.getInputStream();
    }




    protected InputStream executeCommand(QueryProfile profile,String query) throws IOException {

        Runtime rt = Runtime.getRuntime();
        String cmd =  "d:/Tools/grep/bin/grep.exe "+
                            (profile.isIgnoreCase() ? " -i " : "") +
                            (profile.isRegExpression() ? " -P " : " -G ") +
                            " \""+query+"\" "+profile.getFileName();
        Process proc = rt.exec(cmd);

        return proc.getInputStream();
    }


}
