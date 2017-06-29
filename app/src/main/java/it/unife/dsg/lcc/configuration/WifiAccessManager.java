package it.unife.dsg.lcc.configuration;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import it.unife.dsg.lcc.util.Utils;

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
            WifiConfiguration conf = getWifiApConfiguration(networkSSID);
            mWifiManager.addNetwork(conf);

            return (Boolean) mWifiManager.getClass().getMethod("setWifiApEnabled",
					WifiConfiguration.class, boolean.class).invoke(mWifiManager, conf, enabled);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static ScanResult connectToWifiAp(Context context, String networkSSID, String excludeSSID)
    {
    	ScanResult result = null;
    	try {
            WifiManager mWifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
    	
            if (mWifiManager == null) {
                // Device does not support Wi-Fi
            	System.out.println("WifiAccessManager:  Ops, your device does not support Wi-Fi");
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
                    Thread.sleep(3000);

            		List<ScanResult> wifiScanResultList = mWifiManager.getScanResults();

					if(wifiScanResultList.size() > 0) {
						// Filter, take only ssid which starts with "networkSSID"
						ArrayList<ScanResult> wifiFilterList = new ArrayList<ScanResult>();
            			for (int i = 0; i < wifiScanResultList.size(); i++) {
							ScanResult accessPoint = wifiScanResultList.get(i);
							String currentSSID = accessPoint.SSID;

            				if(currentSSID != null && currentSSID.startsWith("\"" + networkSSID) &&
									!currentSSID.equals(excludeSSID)) {
            					wifiFilterList.add(accessPoint);
							}
						}


                        // TEST
                        if(wifiFilterList.size() > 0) {
                            for (ScanResult wifiFilter : wifiFilterList)
                                System.out.println("WifiAccessManager, network name: " + wifiFilter.SSID);
                        } else {
                            System.out.println("WifiAccessManager: NOT found networks with filters");
                        }


						int size = wifiFilterList.size();
						if (size > 0) {
							//Select new access point random
							int n = Utils.nextRandomInt(size);
							ScanResult newAccessPoint = wifiFilterList.get(n);

//            			    for (i = 0; i < wifiScanResultList.size(); i++) {
//            				    ScanResult accessPoint = wifiScanResultList.get(i);
//            				    String currentSSID = accessPoint.SSID;
//                         
//            				if(currentSSID != null && currentSSID.startsWith("\"" +networkSSID) && !currentSSID.equals(excludeSSID)) {
                                WifiConfiguration conf = getWifiApConfiguration(newAccessPoint.SSID);
            					int networkId = mWifiManager.getConnectionInfo().getNetworkId();
            					mWifiManager.removeNetwork(networkId);
            					mWifiManager.saveConfiguration();

                                int netId = mWifiManager.addNetwork(conf);
//                        	    mWifiManager.updateNetwork(conf);
//                        		mWifiManager.saveConfiguration();

            					boolean isDisconnected = mWifiManager.disconnect();
            					System.out.println("WifiAccessManager isDisconnected: " + isDisconnected);

               					boolean isEnabled = mWifiManager.enableNetwork(netId, true);
            					System.out.println("WifiAccessManager isEnabled: " + isEnabled);

            					boolean isReconnected = mWifiManager.reconnect();
            					System.out.println("WifiAccessManager isReconnected: " + isReconnected);

            					if(isReconnected) {
            					    //mWifiManager.saveConfiguration();
            						result = newAccessPoint;
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
    	}
    	catch (Exception e) {
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
    
    private static WifiConfiguration getWifiApConfiguration(String networkSSID) {
        WifiConfiguration conf = new WifiConfiguration();
        //String ssid = convertToQuotedString(networkSSID);
        // Please note the quotes. String should contain ssid in quotes
        conf.SSID = "\"" + networkSSID + "\"";

        conf.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
        return conf;
    }

}
