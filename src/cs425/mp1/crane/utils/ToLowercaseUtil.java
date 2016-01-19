package cs425.mp1.crane.utils;

import com.esotericsoftware.minlog.Log;


/**
 * Created by ctote on 12/4/15.
 */
public class ToLowercaseUtil extends BaseUtil{
    @Override
    public boolean isCollector(){return false;}

    @Override
    public String getName() {
        return "toLowerCaseUtil";
    }

    @Override
    public Integer call() {
        String updatedLine = "";
        while (this.data.peekWork() != null) {
         //   Log.info(this.getName(), String.format("Working on: \t%s", this.data.peekWork()));
            updatedLine = this.data.peekWork().toLowerCase();
            this.data.popWork();
            this.data.pushCompletion(updatedLine);
          //  Log.info(this.getName(), String.format("Line Complete: \t%s", updatedLine));
        }
        return 0;
    }
}