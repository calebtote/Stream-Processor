package cs425.mp1.network;

public class Heartbeat {

        protected String beat = "HB_000";
        protected String ack = "HB_001";
        protected String msg = null;

        public short getMissedBeats() {
            return missedBeats;
        }

        public void setMissedBeats(short missedBeats) {
            this.missedBeats = missedBeats;
        }

        public String getBeat() {
            return beat;
        }

        public void setBeat(String beat) {
            this.beat = beat;
        }

        public String getAck() {
            return ack;
        }

        public void setAck(String ack) {
            this.ack = ack;
        }

        public String getMsg() {
            return msg;
        }

        public void setMsg(String msg) {
            this.msg = msg;
        }

        //       protected Date beatTime = null;
        //       protected Date ackTime  = null;
        protected short missedBeats = 0;
        protected short MAX_MISS = 3;
    
}