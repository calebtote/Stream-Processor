package cs425.mp1.crane.apps;

import cs425.mp1.crane.utils.BaseUtil;
import cs425.mp1.crane.utils.RemovePunctuationUtil;
import cs425.mp1.crane.utils.ToLowercaseUtil;
import cs425.mp1.crane.utils.WordCountUtil;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * Created by ctote on 12/2/15.
 */
public class TestApp extends AppBase {

    public TestApp(){
        this.result = new TreeMap<>();
    }
    private Map<String,Integer> result;

    @Override
    public Map<String, Integer> getResult(){ return result; }

    @Override
    public String getName(){
        return "testApp";
    }

    @Override
    public void setUtilities() {
        this.pushUtility(new ToLowercaseUtil());
      //  this.pushUtility(new RemovePunctuationUtil());
        this.pushUtility(new WordCountUtil());
    }

    @Override
    public void processPendingWork(){

    }
}
