package it.unife.dsg.lcc.util;

import android.content.Context;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
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

    public static String getDate(String format) {
        // example "HH:mm:ss", "yyyy-MM-dd'T'HH:mm:ss"
		SimpleDateFormat dateFormat = new SimpleDateFormat(format, Locale.ITALY);
		return dateFormat.format(new Date());
	}

	public static String readFirstLine(Context context, String filename) {
		String text = "";
		try {
			FileInputStream inputStream = context.openFileInput(filename);
			BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

            text = reader.readLine();
			reader.close();
			inputStream.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return text;
	}

	public static boolean writeFile(Context context, String text) {
        try {
            FileOutputStream fOut = context.openFileOutput(Constants.HOTSPOT_ID, Context.MODE_PRIVATE);
            fOut.write(text.getBytes());
            fOut.close();
            return true;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

	public static void appendLog(String text) {
		File androidShareDirectory = new File(android.os.Environment.getExternalStorageDirectory() + "/wifiOpp");
		if(!androidShareDirectory.exists())
			androidShareDirectory.mkdirs();

		String logDirectory = androidShareDirectory.getAbsolutePath() + "/log";
	    File dir = new File(logDirectory);
		if(!dir.exists())
			dir.mkdir();
	    
	    File logFile = new File(logDirectory+ "/log.txt"); 
	    
	    if (!logFile.exists()) {
			try {
	          logFile.createNewFile();
	       } catch (IOException e) {
	          e.printStackTrace();
	       }
	    }

	    try {
	       // BufferedWriter for performance, true to set append to file flag
	       BufferedWriter buf = new BufferedWriter(new FileWriter(logFile, true)); 
	       
	       Date date=new Date(System.currentTimeMillis());
   		   String log = getDate("yyyy-MM-dd'T'HH:mm:ss") + ": " + text;
   		
	       buf.append(log);
	       buf.newLine();
	       buf.close();
	    } catch (IOException e) {
	       e.printStackTrace();
	    }
	 }
}
