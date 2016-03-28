import java.io.*;
import java.util.*;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.DigestInputStream;
import org.apache.commons.io.IOUtils;
import org.apache.commons.codec.binary.Hex;

public class TarChecker2 {
    final int BLOCK_SIZE = 512;
    final int BUFSIZE = 64*1024;

    HashMap<String, String> path2md5 = new HashMap<String, String>();
    HashMap<String, Boolean> checked = new HashMap<String, Boolean>();
    long pos = 0L;
	
    byte[] BUF = new byte[BUFSIZE];
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
        BufferedInputStream bis = new BufferedInputStream(new FileInputStream(tarPath), BUFSIZE);
        byte[] header = new byte[512];
        byte[] sizebuf = new byte[12];
        byte[] namebuf = new byte[128];
	byte[] linknamebuf = new byte[100];
	byte[] prefixbuf = new byte[155];
int j = 0;
        boolean isLongName = false;
        String longname = null;
        while(true){
                // System.out.println("type:  " + typeflag);
            int read = bis.read(header);
            long pos_tmp = pos;
            pos += read;
            byte typeflag = header[156];

   //       FileOutputStream fout = new FileOutputStream("hoge_java" + j);
   //       j++;
   //       fout.write(header);
   //       fout.close();

            System.arraycopy(header, 124, sizebuf,0, 12);
            System.arraycopy(header, 157, linknamebuf,0, 100);
            System.arraycopy(header, 345, prefixbuf,0, 155);
            // System.arraycopy(header, 124, sizebuf,0, 12);
            System.arraycopy(header, 0, namebuf,0, 100);
            String name = null;
            if(longname != null){
                name = longname;
                longname = null;
            }else {
                name = str(namebuf);
            }
	    String linkname = str(linknamebuf);
            String prefix = str(prefixbuf);

            long length = bytesToLong(sizebuf);
            // System.out.printf("pos: %x\n", pos_tmp);
            // System.out.println("name: "+ name);
            // System.out.println("read: " + read + " " + ((char)typeflag));
            // System.out.println("length: "+ length);
            long block_length = (length + BLOCK_SIZE-1)/BLOCK_SIZE * BLOCK_SIZE;
            // System.out.println("blength: "+ block_length);
            long skip = 0;
            if(name.length() == 0){
                System.err.println("finish");
                break;
            }else if(typeflag == 'L'){ // long link
                isLongName = true;
                skip = block_length;
            }else {
                isLongName = false;
		System.err.println("Checking(name)    : " + name);
		// System.err.println("Checking(linkname): " + linkname);
		// System.err.println("Checking(typeflag): " + ((char)typeflag));
		// System.err.println("Checking(prefix)  : " + prefix);
                if(path2md5.containsKey(name)){
                    String md5 = md5(bis, length);
                    if(path2md5.get(name).equals(md5)){
                        System.out.println("OK:\t" + name + "\t" + md5);
                    }else {
                        System.out.println("Bad:\t" + name + "\t" + md5);
                    }
                    checked.put(name, true);
                    skip = block_length - length;
                }else {
                    skip = block_length;
                }
            }
            while(skip > 0){
                // long skipped = bis.skip(skip);
                long this_skip;
                if(skip > BUFSIZE){
                   this_skip = BUFSIZE;
                }else {
                   this_skip = skip;
                }
                long skipped = (long)bis.read(BUF, 0, (int)this_skip);
                if(isLongName){
                   longname = str(BUF);
                   // System.out.println("longname: "+ longname);
		}
/*
                if(skipped != this_skip){
                    // System.out.println("skip != skipped: " + skip + " "+ skipped);
                }else {
                    // System.out.println("skip == skipped: " + skip + " "+ skipped);
                }
*/
                skip -= skipped;
                pos += skipped;
            }
            // System.out.println("skip " + skip);
        }
        bis.close();
        for(String k: checked.keySet()){
            if(checked.get(k) == false){
                System.out.println("No:\t" + k);
            }
        }

    }
    private String md5(InputStream is, long size)throws IOException, NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("MD5");
        DigestInputStream dis = new DigestInputStream(is, md);
        byte[] buf = new byte[BLOCK_SIZE];
        while(size > 0){
            int len = (size > BLOCK_SIZE)? BLOCK_SIZE: (int)size;
            long read = dis.read(buf, 0, len);
            pos += read;
            size -= read;
        }
        byte[] digest = md.digest();
        return Hex.encodeHexString(digest);
    }
    public long bytesToLong(byte[] bytes) {
        String s = str(bytes);
        s = s.replaceAll("^[0 ]+", "");
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
                    if(buf[1].startsWith("./")){
                        buf[1] = buf[1].substring(2);
                    }
                    path2md5.put(buf[1], buf[0]);
                    checked.put(buf[1], false);
                }
            }
            br.close();
        }
    }
}
