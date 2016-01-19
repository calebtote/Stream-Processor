package cs425.mp1.crane;

import com.esotericsoftware.minlog.Log;

import java.util.Set;
import java.util.concurrent.Future;

/**
 * Created by ctote on 12/4/15.
 */
public class EnQThread extends Thread {
    private final StreamWrapper data;
    public EnQThread(StreamWrapper sw)
    {
        this.data = sw;
    }

    @Override
    public void run()
    {
        Log.info("EnQ-Thread");
        WorkerControl.getGateKeeper().EnqueueWork(this.data);
    }
}
