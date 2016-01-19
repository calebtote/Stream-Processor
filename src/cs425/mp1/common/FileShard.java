package cs425.mp1.common;

import cs425.mp1.network.PeerConnection;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.Serializable;

/**
 * Created by ctote on 10/26/15.
 */
public class FileShard implements Serializable{
    public static final String SHARD_PART_TAG = ".part";
    public static final String SHARD_PART_DIR = "parts";
    public FileShard() {

    }

    public FileShard(String _fileName) {
        try {


            // _fileName of the form xyz.ext.part#
            seqNum = Integer.valueOf(_fileName.substring(_fileName.lastIndexOf('.') + SHARD_PART_TAG.length(), _fileName.length()));
            if (_fileName.contains("\\") )
                shardPath = _fileName.substring(0, _fileName.lastIndexOf("\\")+1);
            else if(_fileName.contains("/"))
                shardPath = _fileName.substring(0, _fileName.lastIndexOf('/')+1);
            else shardPath ="";

            fileName = _fileName.substring(shardPath.length(), _fileName.lastIndexOf('.'));

            File file = new File(_fileName);
            FileInputStream fis = new FileInputStream(file);
            payload = new byte[(int) file.length()];
            int bytesRead = fis.read(payload, 0, (int) file.length());
            assert (bytesRead == payload.length);
            assert (bytesRead == (int) file.length());
            fis.close();
            fis = null;
        } catch (IOException ex) { ex.printStackTrace();}
    }

    public FileShard(String fileName, String shardPath, long seq, byte[] _payload){
        this.seqNum = seq;
        this.fileName = fileName;
        this.shardPath = shardPath;
        this.payload = _payload;
    }

    public String getFileName() { return fileName; }
    public String getShardPath() { return shardPath; }
    public byte[] getPayload() { return payload; }
    public long getSeqNum() { return seqNum; }

    public String toString() { return getFullPath(); }
    public String getFullPath () {
        String fileName = getFileName() + FileShard.SHARD_PART_TAG + getSeqNum();
        String partPath = getShardPath() + File.separator + getFileName().substring(0, getFileName().indexOf(".")) + File.separator + FileShard.SHARD_PART_DIR;
         return partPath + File.separator + fileName;
    }
    //String fullPath;
    String fileName;
    String shardPath;
    long seqNum;
    byte[] payload;

    public static void main(String[] args) {
        FileShard fs = new FileShard("deathstar.jpg.part1");
        System.out.println(fs.getSeqNum());
        System.out.println(fs.getFileName());
        System.out.println(fs.getPayload().length);


        fs = new FileShard("D:\\Box\\Box Sync\\I2CS\\Fall 2015\\CS 425\\Projects\\mp1\\deathstar.jpg.part1");
        System.out.println(fs.getSeqNum());
        System.out.println(fs.getFileName());
        System.out.println(fs.getPayload().length);
        System.out.println(fs);

        fs = new FileShard("D:/Box/Box Sync/I2CS/Fall 2015/CS 425/Projects/mp1/deathstar.jpg.part1");
        System.out.println(fs.getSeqNum());
        System.out.println(fs.getFileName());
        System.out.println(fs.getPayload().length);
        System.out.println(fs);

    }

}
