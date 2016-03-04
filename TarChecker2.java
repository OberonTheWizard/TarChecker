import java.io.*;
import java.util.*;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.DigestInputStream;
import org.apache.commons.io.IOUtils;
import org.apache.commons.codec.binary.Hex;

public class TarChecker2 {

    HashMap<String, String> path2md5 = new HashMap<String, String>();
    HashMap<String, Boolean> checked = new HashMap<String, Boolean>();
    public static void main(String[] argv){
        ArrayList<String> checkListFiles = new ArrayList<String>();
        String tarPath = null;
        for(int i = 0; i<argv.length; i++){
            if(argv[i].equals("-c")){
                checkListFiles.add(argv[i+1]);
                i++;
            }else {
                tarPath = argv[i];
            }
        }
        if(checkListFiles.size() == 0 || tarPath == null){
            System.err.println("Usage: java TarChecker -c md5sum.txt [tar]");
            System.exit(1);
        }
        try {
            TarChecker2 test = new TarChecker2();
            test.loadTable(checkListFiles);
            test.check(tarPath);
        }catch(Exception e){
            System.err.println(e.getMessage());
            e.printStackTrace();
        }
    }
    public void check(String tarPath) throws IOException, NoSuchAlgorithmException{
        DataInputStream dis = new DataInputStream(new BufferedInputStream(new FileInputStream(tarPath)));
        while(dis.available() > 0){
            byte[] header = new byte[512];
            byte[] sizebuf = new byte[12];
            byte[] namebuf = new byte[128];
            byte typeflag = header[156];
                // System.out.println("type:  " + typeflag);
            dis.readFully(header);
            System.arraycopy(header, 124, sizebuf,0, 12);
            // System.arraycopy(header, 124, sizebuf,0, 12);
            System.arraycopy(header, 0, namebuf,0, 100);
            String name = str(namebuf);
            long length = bytesToLong(sizebuf);
            long block_length = (length + 511)/512 * 512;
            long skip = 0;
            if(name.length() == 0){
                break;
            }else {
                if(path2md5.containsKey(name)){
                    String md5 = md5(dis, length);
                    if(path2md5.get(name).equals(md5)){
                        System.err.println("OK:\t" + name + "\t" + md5);
                    }else {
                        System.err.println("Bad:\t" + name + "\t" + md5);
                    }
                    skip = block_length - length;
                }else {
                    skip = block_length;
                }
            }
            while(skip > 0){
                long skipped = dis.skip(skip);
                skip -= skipped;
            }
            // System.out.println("skip " + skip);
        }
        dis.close();
    }
    private String md5(InputStream is, long size)throws IOException, NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("MD5");
        DigestInputStream dis = new DigestInputStream(is, md);
        byte[] buf = new byte[512];
        while(size > 0){
            int len = (size > 512)? 512: (int)size;
            long read = dis.read(buf, 0, len);
            size -= read;
        }
        byte[] digest = md.digest();
        return Hex.encodeHexString(digest);
    }
    public long bytesToLong(byte[] bytes) {
        String s = str(bytes);
        s = s.replaceAll("^0+", "");
        if(s.length() == 0){
            return 0;
        }
        return Long.parseLong(s, 8);
    }
    private String str(byte[] arr){
        int i;
        for (i = 0; i < arr.length && arr[i] != 0; i++) { }
        return new String(arr, 0, i);
    }
    private void loadTable(ArrayList<String> paths) throws IOException{
        for(String path: paths) {
            BufferedReader br = new BufferedReader(new FileReader(path));
            String raw = null;
            while(null != (raw = br.readLine())){
                String[] buf = raw.split("\\s+", 2);
                if(buf.length > 1){
                    path2md5.put(buf[1], buf[0]);
                    checked.put(buf[1], false);
                }
            }
            br.close();
        }
    }
}
