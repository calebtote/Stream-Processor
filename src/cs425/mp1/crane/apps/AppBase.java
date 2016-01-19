package cs425.mp1.crane.apps;

import cs425.mp1.common.FileShard;
import cs425.mp1.crane.StreamWrapper;
import cs425.mp1.crane.utils.BaseUtil;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * Created by ctote on 12/2/15.
 */
abstract public class AppBase {
    public AppBase(){
        utilities = new ArrayDeque<>();
        utilNameMap = new HashMap<>();

        name = getName();
    }

    private String name;
    private String dataPath;
    public List<FileShard> data;
    public Deque<BaseUtil> utilities;
    public Deque<BaseUtil> getUtilities(){ return utilities; }
    public BaseUtil popUtility(){ return utilities.pop(); }
    public Map<String, BaseUtil> utilNameMap;
    protected Deque<String> pendingWork = new ConcurrentLinkedDeque<String>();
    public void pushUtility(BaseUtil util){
        utilities.push(util);
        utilNameMap.put(util.getName(), util);
    }

    abstract public String getName();
    abstract public void setUtilities();

    public void setDataPath(String path){ this.dataPath = path; }
    public String getDataPath(){ return dataPath; }

    abstract public Map<String, Integer> getResult();
    abstract public void processPendingWork();
}
