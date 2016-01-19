package cs425.mp1.main;

import java.io.*;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.util.Properties;

/**
 */
public class ConfigManager {
    public static void main(String[] args) {
        createDefaultProperties();
    }

    public static Properties getProperties() {
        Properties prop = new Properties();
        InputStream input = null;

        try {
            String path = URLDecoder.decode(UX.class.getProtectionDomain().getCodeSource().getLocation().getPath(), "UTF-8");
            String decodedPath = (new File(path)).getParentFile().getPath();
            input = new FileInputStream(decodedPath + "/config.properties");

            // load a properties file
            prop.load(input);
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return prop;
    }

    public static void createDefaultProperties() {
        Properties prop = new Properties();
        OutputStream output = null;

        try {
            output = new FileOutputStream("config.properties");

           /* // set the properties value
            prop.setProperty("host1", "fa15-cs425-g27-01.cs.illinois.edu");
            prop.setProperty("host2", "fa15-cs425-g27-02.cs.illinois.edu");
            prop.setProperty("host3", "fa15-cs425-g27-03.cs.illinois.edu");
            prop.setProperty("host4", "fa15-cs425-g27-04.cs.illinois.edu");
            prop.setProperty("host5", "fa15-cs425-g27-05.cs.illinois.edu");
            prop.setProperty("host6", "fa15-cs425-g27-06.cs.illinois.edu");
            prop.setProperty("host7", "fa15-cs425-g27-07.cs.illinois.edu");
            prop.setProperty("inputFile", "samples/sample.log");
            prop.setProperty("query", "index");
*/
            // save properties to project root folder
            prop.store(output, null);

        } catch (IOException io) {
            io.printStackTrace();
        } finally {
            if (output != null) {
                try {
                    output.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        }
    }
}
