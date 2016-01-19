package cs425.mp1.common;

import java.util.*;

/**
 */
public class QueryResults {
    protected Set<String> resultList = new HashSet<String>();
    protected double executionTime = 0;

    public double getExecutionTime() { return executionTime; }
    public void setExecutionTime(double executionTime) {
        this.executionTime = executionTime;
    }
    public void addResultLine(String line){ resultList.add(line); }
    public int getResultsCount() {
        return resultList.size();
    }

    public String getResults() {
        StringBuffer output = new StringBuffer();

        for (String result : resultList)
            output.append(result+"\n");

        return output.toString();
    }
}
