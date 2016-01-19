package cs425.mp1.common;

/**
 * This class contains the query profile to be exchanged between the query master and query agents
 */
public class QueryProfile {

    protected String fileName = null;
    protected String hostName = null;
    protected boolean ignoreCase = false;
    protected boolean isRegExpression = false;
    protected String query = "";    // TODO: this is temp set in the profile

    public boolean isRegExpression() { return isRegExpression; }
    public void setIsRegExpression(boolean isRegExpression) {
        this.isRegExpression = isRegExpression;
    }

    public boolean isIgnoreCase() {
        return ignoreCase;
    }
    public void setIgnoreCase(boolean ignoreCase) {
        this.ignoreCase = ignoreCase;
    }

    public String getQuery() { return query; }
    public void setQuery(String query) {
        this.query = query;
    }

    public String getHostName() {
        return hostName;
    }
    public void setHostName(String hostName) { this.hostName = hostName; }

    public String getFileName() {
        return fileName;
    }
    public void setFileName(String fileName) {
        this.fileName = fileName;
    }
}
