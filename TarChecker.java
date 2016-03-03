import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.tar.*;
import java.util.HashMap;
import java.io.FileReader;
import java.io.File;
import java.io.BufferedReader;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;

public class TarChecker {

    HashMap<String, String> path2md5 = new HashMap<String, String>();
    HashMap<String, Boolean> checked = new HashMap<String, Boolean>();

    public static void main(String[] argv){
        String checkListFile = null;
        String tarPath = null;
        for(int i = 0; i<argv.length; i++){
            if(argv[i].equals("-c")){
                checkListFile = argv[i+1];
                i++;
            }else {
                tarPath = argv[i];
            }
        }
        if(checkListFile == null || tarPath == null){
            System.err.println("Usage: java TarChecker -c md5sum.txt [tar]");
            System.exit(1);
        }
        try {
            TarChecker test = new TarChecker();
            test.loadTable(checkListFile);
            test.check(tarPath);
        }catch(Exception e){
            System.err.println(e.getMessage());
            e.printStackTrace();
        }
    }
    public void check(String tarPath) throws IOException{
        // System.err.println(tarPath);
        // System.err.println(checkListFile);
        TarArchiveInputStream tis = null;
        try {
            tis = new TarArchiveInputStream(new BufferedInputStream(new FileInputStream(tarPath)));
            TarArchiveEntry ae = null;
            while(null != (ae = tis.getNextTarEntry())) {
                if(path2md5.containsKey(ae.getName()) && !ae.isDirectory()){
                    String md5 = org.apache.commons.codec.digest.DigestUtils.md5Hex(tis);
                    if(path2md5.get(ae.getName()).equals(md5)){
                        System.err.println("OK:\t" + ae.getName());
                    }else {
                        System.err.println("Bad:\t" + ae.getName());
                    }
                    // String md5 = md5(ae.getFile());
                    // System.out.println(md5 + "\t" + ae.getName());
                    checked.put(ae.getName(), true);
                }
            }
            for(String k: checked.keySet()){
                if(checked.get(k) == false){
                    System.err.println("No:\t" + k);
                }
            }
        }finally {
            if(tis != null){
                tis.close();
            }
        }
    }
    private String md5(File file) throws IOException{
        if(file == null){
            System.err.print(" file is null");
        }
        return org.apache.commons.codec.digest.DigestUtils.md5Hex(new FileInputStream(file));
    }
    private void loadTable(String path) throws IOException{
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
