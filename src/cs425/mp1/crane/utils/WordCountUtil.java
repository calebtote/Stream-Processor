package cs425.mp1.crane.utils;

import com.esotericsoftware.minlog.Log;
import cs425.mp1.crane.WorkerControl;


/**
 * Created by ctote on 12/2/15.
 */
public class WordCountUtil extends BaseUtil {

    @Override
    public boolean isCollector(){return true;}

    @Override
    public String getName() {
        return "wordCountUtil";
    }

    @Override
    public Integer call() {

        int count = 0;

        while (this.data.peekWork() != null) {
        //    Log.info(this.getName(), String.format("Working on: \t%s", this.data.peekWork()));
            String trim = this.data.popWork().trim();
            if (trim.isEmpty())
                continue;
            String[] words = trim.split("\\W+");
            for (String word : words){
                count = (WorkerControl.getApp().getResult()).containsKey(word) ? (WorkerControl.getApp().getResult()).get(word) : 0;
                (WorkerControl.getApp().getResult()).put(word, count + 1);
            }
        }

        return count;
    }
}
