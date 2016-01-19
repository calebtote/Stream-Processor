package cs425.mp1.crane.utils;

import cs425.mp1.crane.StreamWrapper;

import java.lang.reflect.Method;
import java.util.concurrent.Callable;

/**
 * Created by ctote on 12/2/15.
 */
abstract public class BaseUtil implements Callable{
    private static String name;
    public StreamWrapper data;
    public void setData(StreamWrapper stream){ this.data = stream; }

    abstract public boolean isCollector();

    abstract public String getName();
    abstract public Integer call();

    public BaseUtil(){
        name = getName();
    }
}