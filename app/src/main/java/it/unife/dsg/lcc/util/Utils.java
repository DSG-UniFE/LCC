package it.unife.dsg.lcc.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Random;

/**
*
* @author Stefano Lanzone
*/
public class Utils {

	private static SecureRandom sr;
	
	private static synchronized SecureRandom getSecureRandom(){
		if(Utils.sr == null){
			try {
				Utils.sr = SecureRandom.getInstance("SHA1PRNG");
			} catch (NoSuchAlgorithmException e) {
				e.printStackTrace();
			}
		}
		return Utils.sr;
	}
	
	public static int nextRandomInt(){
		Random random = Utils.getSecureRandom();
    	return random.nextInt();
    }
	
	public static int nextRandomInt(int n){
		Random random = Utils.getSecureRandom();
    	return random.nextInt(n);
    }

    public static short nextRandomShort(){
        Random random = Utils.getSecureRandom();
        return (short) random.nextInt(Short.MAX_VALUE + 1);
    }

    public static short nextRandomNonNegativeShort(){
        Random random = Utils.getSecureRandom();
        return (short) random.nextInt(1 << 15);
    }

    public static String getDate() {
		SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss", Locale.ITALY);
		return dateFormat.format(new Date());
	}

	 public static void appendLog(String text)
	 { 
		File androidShareDirectory = new File(android.os.Environment.getExternalStorageDirectory() + "/wifiOpp");
		if(!androidShareDirectory.exists())
			androidShareDirectory.mkdirs();
		
		String logDirectory = androidShareDirectory.getAbsolutePath() + "/log";
	    File dir = new File(logDirectory);
	    if(!dir.exists())
	    	dir.mkdir();
	    
	    File logFile = new File(logDirectory+ "/log.txt"); 
	    
	    if (!logFile.exists())
	    {
	       try
	       {
	          logFile.createNewFile();
	       } 
	       catch (IOException e)
	       {
	          // TODO Auto-generated catch block
	          e.printStackTrace();
	       }
	    }
	    try
	    {
	       //BufferedWriter for performance, true to set append to file flag
	       BufferedWriter buf = new BufferedWriter(new FileWriter(logFile, true)); 
	       
	       Date date=new Date(System.currentTimeMillis());
   		   String log = date.toLocaleString() +": "+text;
   		
	       buf.append(log);
	       buf.newLine();
	       buf.close();
	    }
	    catch (IOException e)
	    {
	       // TODO Auto-generated catch block
	       e.printStackTrace();
	    }
	 }
}
