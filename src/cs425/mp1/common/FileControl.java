package cs425.mp1.common;

import com.esotericsoftware.minlog.Log;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


/**
 * Created by ctote on 10/26/15.
 */
public class FileControl {
    static public boolean DeleteDirectory(File path) {
        if (path.exists()) {
            File[] files = path.listFiles();
            for (int i = 0; i < files.length; i++) {
                if (files[i].isDirectory()) {
                    DeleteDirectory(files[i]);
                } else {
                    files[i].delete();
                }
            }
        }
        return (path.delete());
    }

    public static List<FileShard> ShardFile(String filePath) throws Exception {
        RandomAccessFile raf = new RandomAccessFile(filePath, "r");

        long OneKiloByte = 1024;
        // shard size
        long bytesPerSplit = OneKiloByte * OneKiloByte; //1MB
        long sourceSize = raf.length();
        long numSplits = sourceSize / bytesPerSplit;
        long remainingBytes = (numSplits > 0) ? sourceSize % (bytesPerSplit) : sourceSize;

        // maximum size that we'll pull into memory at one time when
        // sharding files into multiple parts
        int maxReadBufferSize = 100 * 1024; //0.1MB

        List<FileShard> fileShards = new ArrayList<FileShard>();
        String fileName = new File(filePath).getName();
        String shardPath = new File(new File(fileName).getAbsolutePath()).getParent();
        String shardTmp = String.format("%s/%s/tmp", shardPath, fileName.substring(0, fileName.indexOf(".")));
        String shardFullPath;

        File f = new File(shardTmp);
        Log.info(shardTmp);
        if (!(f.mkdirs())){
            Log.info("ShardFile", String.format("Path %s exists. Cleaning up...", shardTmp));
            DeleteDirectory(f);
            f.mkdirs();
        }

        byte[] payload = null;

        for (int shardSeqNum = 1; shardSeqNum <= numSplits; shardSeqNum++) {
            // example: /home/tote2/artifacts/testimage/tmp/testimage.jpg.part1
            shardFullPath = String.format("%s/%s%s%d", shardTmp, fileName, FileShard.SHARD_PART_TAG, shardSeqNum);
            Log.info("ShardFullPath", shardFullPath);

            BufferedOutputStream bw = new BufferedOutputStream(new FileOutputStream(shardFullPath));
            ByteArrayOutputStream payloadOutputStream = new ByteArrayOutputStream();

            if (bytesPerSplit > maxReadBufferSize) {
                long numReads = bytesPerSplit / maxReadBufferSize;
                long numRemainingRead = bytesPerSplit % maxReadBufferSize;
                for (int i = 0; i < numReads; i++) {
                    payloadOutputStream.write(readWrite(raf, bw, maxReadBufferSize));
                }
                if (numRemainingRead > 0) {
                    payloadOutputStream.write(readWrite(raf, bw, numRemainingRead));
                }
            } else {
                payloadOutputStream.write(readWrite(raf, bw, bytesPerSplit));
            }

            bw.close();
            fileShards.add(new FileShard(fileName, shardPath, shardSeqNum, payloadOutputStream.toByteArray()));
        }

        if (remainingBytes > 0) {
            // example: /home/tote2/artifacts/testimage/tmp/testimage.jpg.part1
            shardFullPath = String.format("%s/%s/tmp/%s%s%d", shardPath, fileName.substring(0, fileName.indexOf(".")), fileName, FileShard.SHARD_PART_TAG, (numSplits + 1));
            Log.debug("ShardFullPath", shardFullPath);
            new File(shardFullPath.substring(0, shardFullPath.lastIndexOf("/"))).mkdirs();

            BufferedOutputStream bw = new BufferedOutputStream(new FileOutputStream(shardFullPath));
            ByteArrayOutputStream payloadOutputStream = new ByteArrayOutputStream();
            payloadOutputStream.write(readWrite(raf, bw, remainingBytes));
            bw.close();
            fileShards.add(new FileShard(fileName, shardPath, numSplits + 1, payloadOutputStream.toByteArray()));
        }
        raf.close();
        return fileShards;
    }

    static byte[] readWrite(RandomAccessFile raf, BufferedOutputStream bw, long numBytes) throws IOException {
        byte[] buf = new byte[(int) numBytes];
        int val = raf.read(buf);
        if (val != -1) {
            bw.write(buf);
        }
        return buf;
    }


    public static void mergeFiles(List<FileShard> shards, String outFile) throws IOException, ClassNotFoundException {
        File ofile = new File(outFile);
        FileOutputStream fos;

        byte[] fileBytes;
        try {
            fos = new FileOutputStream(ofile, true);
            for (FileShard shard : shards) {

                fileBytes = shard.getPayload();

                fos.write(fileBytes);
                fos.flush();
                fileBytes = null;

            }
            fos.close();
            fos = null;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

//    public static void main(String[] args) {
//        String in = "deathstar.jpg";
//        String out = "out.jpg";
//        try {
//            List<FileShard> shardList = ShardFile(in);
//            mergeFiles(shardList, out);
//        } catch (Exception e) {e.printStackTrace();}
//    }
}
