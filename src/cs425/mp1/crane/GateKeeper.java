package cs425.mp1.crane;

import com.esotericsoftware.minlog.Log;
import cs425.mp1.crane.utils.BaseUtil;
import cs425.mp1.network.Node;
import cs425.mp1.network.StorageTopology;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.*;

/**
 * Created by ctote on 12/2/15.
 */
public class GateKeeper {
    GateKeeper(BaseUtil util) {
        this.util = util;
    }

    private final BaseUtil util;
    public BaseUtil getUtil(){ return util; }
    private final ExecutorService pool = Executors.newFixedThreadPool(5);

    public Set<Future> EnqueueWork(StreamWrapper stream) {
        Log.info("Enqueue-Work");
        Set<Future> futureObjs = new HashSet<>();
        util.setData(stream);
        Callable callable = util;

        // This does the work.
        Future future = pool.submit(callable);
        futureObjs.add(future);

        //TODO: Should probably get this working at some point... but not super important right now.
       //     pool.shutdown();
       //     try {
       //         pool.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
       //     } catch (InterruptedException e) {
       //         e.printStackTrace();
       //     }


        try {
            future.get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }

        Node.sendTCP(WorkerControl.getNextHop(), WorkerControl.prepareForTransport(stream));

        return futureObjs;
    }
}
