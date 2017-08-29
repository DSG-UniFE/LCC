package it.unife.dsg.lcc.configuration;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import it.unife.dsg.lcc.util.Utils;

import static android.text.TextUtils.isEmpty;

/**
*
* @author Stefano Lanzone
*/

public class WifiAccessManager {

    public static boolean setWifiApState(Context context, String networkSSID, boolean enabled) {
        try {
            WifiManager mWifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            
            // TURN OFF WIFI BEFORE ENABLE HOTSPOT
            if (enabled) {
                mWifiManager.setWifiEnabled(false);
            }
            WifiConfiguration conf = getWifiApConfiguration(context, networkSSID);
            mWifiManager.addNetwork(conf);

            return (Boolean) mWifiManager.getClass().getMethod("setWifiApEnabled",
					WifiConfiguration.class, boolean.class).invoke(mWifiManager, conf, enabled);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static ScanResult connectToWifiAp(Context context, String networkSSID, String excludeSSID) {
//        System.out.println("WifiAccessManager, networkSSID:  '" + networkSSID + "',  excludeSSID: '" + excludeSSID + "'");
    	ScanResult result = null;
    	try {
            WifiManager mWifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
    	
            if (mWifiManager == null) {
                // Device does not support Wi-Fi
            	System.out.println("WifiAccessManager:  Ops, your device does not support Wi-Fi");
                Toast.makeText(context, "Ops, your device does not support Wi-Fi",
                        Toast.LENGTH_LONG).show();
            } else {
//            	if (connect) {
					if (!mWifiManager.isWifiEnabled()) {
            			// To turn on Wi-Fi
            			Method method = mWifiManager.getClass().getMethod("isWifiApEnabled");
            			boolean wifiApEnabled = Boolean
            					.parseBoolean(method.invoke(mWifiManager).toString());
            			if(wifiApEnabled)
            				setWifiApState(context, networkSSID, false);

            			mWifiManager.setWifiEnabled(true);
//            			mWifiManager.startScan();
            			//Thread.sleep(2500);
            		}
                    mWifiManager.startScan();

                    // TODO
                    /* Wrong method.
                    Right method:
                    https://stackoverflow.com/questions/18741034/how-to-get-available-wifi-networks-and-display-them-in-a-list-in-android
                     */
                    Thread.sleep(3000);

            		List<ScanResult> wifiScanResultList = mWifiManager.getScanResults();

					if(wifiScanResultList.size() > 0) {
						// Filter, take only ssid which starts with "networkSSID"
						ArrayList<ScanResult> wifiFilterList = new ArrayList<ScanResult>();
            			for (int i = 0; i < wifiScanResultList.size(); i++) {
							ScanResult accessPoint = wifiScanResultList.get(i);
							String currentSSID = accessPoint.SSID;

            				if(currentSSID != null && currentSSID.contains(networkSSID) &&
									!currentSSID.equals(excludeSSID)) {
            					wifiFilterList.add(accessPoint);
							}
						}

						int size = wifiFilterList.size();

						if (size > 0) {
							// Select new access point random
							int n = Utils.nextRandomInt(size);
							ScanResult newAccessPoint = wifiFilterList.get(n);

//            			    for (i = 0; i < wifiScanResultList.size(); i++) {
//            				    ScanResult accessPoint = wifiScanResultList.get(i);
//            				    String currentSSID = accessPoint.SSID;
//                         
//            				if(currentSSID != null && currentSSID.startsWith("\"" +networkSSID) && !currentSSID.equals(excludeSSID)) {
                                WifiConfiguration conf = getWifiApConfiguration(context, newAccessPoint.SSID);
            					int networkId = mWifiManager.getConnectionInfo().getNetworkId();

                                boolean isDisconnected = mWifiManager.disconnect();
                                mWifiManager.removeNetwork(networkId);
            					mWifiManager.saveConfiguration();

                                int netId = mWifiManager.addNetwork(conf);
//                        	    mWifiManager.updateNetwork(conf);
//                        		mWifiManager.saveConfiguration();

               					boolean isEnabled = mWifiManager.enableNetwork(netId, true);
            					boolean isReconnected = mWifiManager.reconnect();

            					if(isReconnected) {
            					    //mWifiManager.saveConfiguration();
            						result = newAccessPoint;
//                                    System.out.println("WifiAccessManager.connectToWifiAp result: " + newAccessPoint.SSID);
//            						break;
            					}
//            				}
                        }
                    }
                }
//            	}
//                else
//                {
//                	 // To turn off Wi-Fi
//                	mWifiManager.setWifiEnabled(false);
//                	result = true;
//                }
//            }
            
            
//            if (connect) {
//            	WifiConfiguration conf = getWifiApConfiguration(networkSSID);
//            	mWifiManager.addNetwork(conf);
//            
//            	List<WifiConfiguration> list = mWifiManager.getConfiguredNetworks();
//            	for( WifiConfiguration i : list ) {
//            		if(i.SSID != null && i.SSID.contains("\"" + networkSSID + "\"")) {
//            			boolean isDisconnected = mWifiManager.disconnect();
//            			System.out.println("WifiAccessManager isDisconnected : " + isDisconnected);
//            			
//            			boolean isEnabled = mWifiManager.enableNetwork(i.networkId, true);
//            			System.out.println("WifiAccessManager isEnabled : " + isEnabled);
//            			
//            			boolean isReconnected = mWifiManager.reconnect();  
//                	    System.out.println("WifiAccessManager isReconnected : " + isReconnected);
//                	    result = true;
//                    break;
//                }           
//             }
//            }
//            
//    	    return result;
    	} catch (Exception e) {
            e.printStackTrace();
        }
    	
    	return result;
    }
    
//    public static boolean isWifiApConnect(Context context, String networkSSID)
//    {
//    	boolean result = false;
//    	try {
//            WifiManager mWifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
//    	
//            if (mWifiManager != null) {
////            	String currentSSID = mWifiManager.getConnectionInfo().getSSID();
////            	if(currentSSID.equals(networkSSID))
////            	{
//            		ConnectivityManager connMgr = (ConnectivityManager)
//            				context.getSystemService(Context.CONNECTIVITY_SERVICE);
//            		
//            		NetworkInfo networkInfo = connMgr.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
//            		boolean isWifiConn = networkInfo.isConnected();
//                    result = isWifiConn;
//                    }
////            	}
//    	}
//        catch (Exception e) {
//            e.printStackTrace();
//        }
//    	
//    	return result;
//    }

    public static String getCurrentSSID(Context context) {
        String ssid = null;
        ConnectivityManager connManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = connManager.getActiveNetworkInfo();
        if (activeNetwork != null) { // connected to the internet
            if (activeNetwork.getType() == ConnectivityManager.TYPE_WIFI) {
                final WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
                final WifiInfo connectionInfo = wifiManager.getConnectionInfo();
                if (connectionInfo != null && (!connectionInfo.getSSID().isEmpty())) {
                    ssid = connectionInfo.getSSID();
                }
            }
        }
        return ssid;
    }

    private static WifiConfiguration getWifiApConfiguration(Context context, String networkSSID) {
        WifiConfiguration conf = new WifiConfiguration();
        // String ssid = convertToQuotedString(networkSSID);
        // Please note the quotes. String should contain ssid in quotes
        conf.SSID = "\"" + networkSSID + "\"";

        conf.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
        assignHighestPriority(context, conf);
        return conf;
    }

    // To tell OS to give preference to this network
    private static void assignHighestPriority(Context context, WifiConfiguration config) {
        WifiManager mWifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        List<WifiConfiguration> configuredNetworks = mWifiManager.getConfiguredNetworks();
        if (configuredNetworks != null) {
            for (WifiConfiguration existingConfig : configuredNetworks) {
                if (config.priority <= existingConfig.priority) {
                    config.priority = existingConfig.priority + 1;
                }
            }
        }
    }

    private static boolean enableNetwork(Context context, String SSID, int networkId) {
        WifiManager mWifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        if (networkId == -1) {
            networkId = getExistingNetworkId(context, SSID);

            if (networkId == -1) {
//                System.out.println("WifiAccessManager.enableNetwork couldn't add network with SSID: " + SSID);
                return false;
            }
        }
        return mWifiManager.enableNetwork(networkId, true);
    }

    private static int getExistingNetworkId(Context context, String SSID) {
        WifiManager mWifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        List<WifiConfiguration> configuredNetworks = mWifiManager.getConfiguredNetworks();
        if (configuredNetworks != null) {
            for (WifiConfiguration existingConfig : configuredNetworks) {
                if (areEqual(trimQuotes(existingConfig.SSID), trimQuotes(SSID))) {
                    return existingConfig.networkId;
                }
            }
        }
        return -1;
    }

    private static String trimQuotes(String str) {
        if (!isEmpty(str)) {
            return str.replaceAll("^\"*", "").replaceAll("\"*$", "");
        }

        return str;
    }

    private static boolean areEqual(String str1, String str2) {
        return str1.equalsIgnoreCase(str2);
    }
}
