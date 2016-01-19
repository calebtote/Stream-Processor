package cs425.mp1.crane;

import java.util.Deque;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * Created by ctote on 12/3/15.
 */
public class StreamWrapper {
    private Deque<String> workingDeque = new ConcurrentLinkedDeque<String>();
    private Deque<String> completionDeque = new ConcurrentLinkedDeque<String>();

    public void setWorkingDeque(Deque<String> dq){ workingDeque = dq; }
    public Deque<String> getWorkingDeque(){ return workingDeque; }
    public void pushWork(String line){ workingDeque.push(line); }
    public String popWork(){ return workingDeque.pop(); }
    public String peekWork(){ return workingDeque.peek(); }

    public void setCompletionDeque(Deque<String> dq){ completionDeque = dq; }
    public Deque<String> getCompletionDeque(){ return completionDeque; }
    public void pushCompletion(String line){ completionDeque.push(line); }
    public String popCompletion(){ return completionDeque.pop(); }
    public String peekCompletion(){ return completionDeque.peek(); }
}
