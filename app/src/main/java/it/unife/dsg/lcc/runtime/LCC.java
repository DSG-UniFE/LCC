package it.unife.dsg.lcc.runtime;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.ScanResult;
import android.support.v4.app.NotificationCompat;

import it.unife.dsg.lcc.MainActivity;
import it.unife.dsg.lcc.R;
import it.unife.dsg.lcc.configuration.BluetoothTethering;
import it.unife.dsg.lcc.configuration.WifiAccessManager;
import it.unife.dsg.lcc.util.Constants;
import it.unife.dsg.lcc.util.Utils;

/**
*
* @author Stefano Lanzone
*/
public class LCC extends Thread {
	
//	private static LCC wifiOpp = null;
	private boolean active = true;
	private String networkSSID, prefixNetworkSSID;
	private Context context;
	private BluetoothTethering bt;
	private BluetoothDevice currentBtHotspot;
	private ScanResult currentWifiHotspot;
	
	private HotspotType hotspotType;
	private LCCRole role;
	private int changeRolePeriod;    //seconds, hotspot --> client and client --> hotspot
	private int changeHotspotPeriod; //seconds, when device has client role
	private int maxTimeWaitToBecomeHotspot;

    private NotificationManager notificationManager;
    // Unique Identification Number for the Notification.
    // We use it on Notification start, and to cancel it.
    static private final int WIFI_ACTIVE_NOTIFICATION_ID = R.string.wifi_notification_id;
    static private final int BLUETOOTH_ACTIVE_NOTIFICATION_ID = R.string.bluetooth_notification_id;


	// Getters and Setters
	public int getChangeRolePeriod() {

        return changeRolePeriod;
	}

	public void setChangeRolePeriod(int changeRolePeriod) {

        this.changeRolePeriod = changeRolePeriod;
	}

	public int getHotspotPeriod() {

        return changeHotspotPeriod;
	}

	public void setHotspotPeriod(int changeHotspotPeriod) {

        this.changeHotspotPeriod = changeHotspotPeriod;
	}

	public HotspotType getHotspotType() {

        return hotspotType;
	}

	public void setHotspotType(HotspotType hotspotType) {

        this.hotspotType = hotspotType;
	}

	//getInstance
//	public static synchronized LCC getInstance(boolean forceStart, Context context,
//			LCCRole initialRole, HotspotType hotspotType) {
//		if (forceStart && wifiOpp == null) {
//			wifiOpp = new LCC(context, initialRole, hotspotType);
//			
//			wifiOpp.start();
//				
//			System.out.println("LCC " + connection + ": ENABLED");
//		}
//		else
//		{
//			wifiOpp.context = context;
//			wifiOpp.role = initialRole;
//			wifiOpp.hotspotType = hotspotType;
//		}
//		
//		return wifiOpp;
//	}
	
	public LCC(Context context, LCCRole role, HotspotType hotspotType, int rs, int hc,
				   int maxTimewaitToBecomeHotspot) {
        prefixNetworkSSID = "RAMP hotspot ";
        networkSSID = prefixNetworkSSID + Utils.nextRandomInt();
        bt = null;
        currentBtHotspot = null;
        currentWifiHotspot = null;

        notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        this.context = context;
		this.role = role;
		this.hotspotType = hotspotType;
		this.changeRolePeriod = rs; // regular = 450
		this.changeHotspotPeriod = hc; // regular = 150
		this.maxTimeWaitToBecomeHotspot = maxTimewaitToBecomeHotspot; // regular = 30

//		this.changeRolePeriod = 120;   
//		this.changeHotspotPeriod = 30;
//		this.maxTimeWaitToBecomeHotspot = 20; 
		
		this.start();
	}
	
	@Override
	public void run() {
		try {
			System.out.println("LCC " + hotspotType + ": START");
			Utils.appendLog("LCC " + hotspotType + " START: initial role " + role.toString());

            showNotification();
            sendIntentBroadcast(Constants.MESSAGE_WIFIOPP_ACTIVATE);

			//Set initial role
			if(role == LCCRole.HOTSPOT)
               setHotspot();
//			else
//				setClient();
			
			long currentTimeChangeRole = changeRolePeriod * 1000; //ms
			long currentTimeChangeHotspot = changeHotspotPeriod * 1000; //ms
			long sleep = 5000;

			while (active) {
				long preResolve = System.currentTimeMillis();
				
				if(currentTimeChangeRole <= 0) {
					Utils.appendLog("LCC " + hotspotType.toString() + ": " +
                            "Timeout changeRolePeriod");
					if(changeRole())
						currentTimeChangeRole = changeRolePeriod * 1000; //restart counter
				}
				else {
					if(currentTimeChangeHotspot <= 0) {
						Utils.appendLog("LCC: " + hotspotType.toString() + ": " +
                                "Timeout changeHotspotPeriod");
						boolean changedHotspot = changeHotspot();
						//if(changedHotspot)
							currentTimeChangeHotspot = changeHotspotPeriod * 1000; //restart counter
					} else {
						if(role == LCCRole.CLIENT) {
							//CLIENT: currentTimeChangeHotspot > 0 && currentTimeChangeRole > 0
							//Check connection to hotspot
							boolean notFound = checkHotspotConnection();
							//boolean notFound = setClient();
							
							if(notFound) {
								Utils.appendLog("LCC: " + hotspotType.toString() + ": " +
                                        "checkHotspotConnection() not found connection!");
								//After a random time changeRole to become hotspot
								int randomTime = Utils.nextRandomInt(maxTimeWaitToBecomeHotspot);

								Utils.appendLog("LCC: " + hotspotType.toString() +
                                        ": " + "Wait " + randomTime +
                                        " seconds before change role...");
								Thread.sleep(randomTime * 1000);
								
								notFound = checkHotspotConnection(); //First new checkHotspotConnection!
								if (notFound) {
									boolean changedRole = changeRole();
									if (changedRole)
										currentTimeChangeRole = changeRolePeriod * 1000; //restart counter
								}
							}
						}
					}
				}
				
				long elapsedResolve = System.currentTimeMillis() - preResolve;
				if (sleep - elapsedResolve > 0)
					Thread.sleep(sleep - elapsedResolve);
				
				// Update counters
				elapsedResolve = System.currentTimeMillis() - preResolve;
				currentTimeChangeRole = currentTimeChangeRole - elapsedResolve;
				currentTimeChangeHotspot = currentTimeChangeHotspot - elapsedResolve;
			}
			System.out.println("LCC " + hotspotType + ": FINISHED");
			Utils.appendLog("LCC " + hotspotType + ": FINISHED");
		} catch (InterruptedException ie) {
			System.out.println("LCC: InterruptedException");
            sendIntentBroadcast(Constants.MESSAGE_WIFIOPP_DEACTIVATE);
		} catch (Exception e) {
            System.out.println("ERROR: Exceptions");
			e.printStackTrace();
		}
		
//		wifiOpp = null;
		System.out.println("LCC " + hotspotType + ": END");
		Utils.appendLog("LCC " + hotspotType + ": END");
	}

	private boolean checkHotspotConnection() {
		boolean notFound = false;
		
		if(hotspotType == HotspotType.WIFI) {
			if(currentWifiHotspot == null) {
				//No connected to hotspot WIFI
				currentWifiHotspot = WifiAccessManager.connectToWifiAp(context, prefixNetworkSSID, "");
				notFound = currentWifiHotspot == null;
			}

//			if(currentWifiHotspot == null || !WifiAccessManager.isWifiApConnect(context, currentWifiHotspot.SSID))
//			{
//				//No connected to hotspot WIFI
//				currentWifiHotspot = WifiAccessManager.connectToWifiAp(context, prefixNetworkSSID, "");
//				notFound = currentWifiHotspot == null;
//			}
			
		}
		else if(hotspotType == HotspotType.BLUETOOTH) {
			if(currentBtHotspot == null) {
				if(bt == null)	
					bt = new BluetoothTethering(context);
				
				//No connected to hotspot BLUETOOTH
				currentBtHotspot = bt.startConnection(prefixNetworkSSID, "");
				notFound = currentBtHotspot == null;
			}
		}
		return notFound;
	}
	
	private boolean changeRole() {
		boolean res = false;
		Utils.appendLog("LCC " + hotspotType.toString() + ": " + "changeRole()");
		
		if(role == LCCRole.CLIENT) {
            //client --> hotspot
            res = setHotspot();
            if(res) {
                //my role is hotspot
                role = LCCRole.HOTSPOT;
            }

            currentBtHotspot = null;
            currentWifiHotspot = null;
		}
		else if(role == LCCRole.HOTSPOT) {
            //hotspot --> client
            res = setClient();
            if(res) {
                // my role is client
                role = LCCRole.CLIENT;
            }
		}
		
		if(res) {
            Utils.appendLog("LCC " + hotspotType.toString() + ": " + "Changed role to " +
                    role.toString());
            sendIntentBroadcast(Constants.MESSAGE_ROLE_CHANGED);
        } else {
            Utils.appendLog("LCC " + hotspotType.toString() + ": " + "Role not changed!");
        }

		return res;
	}

	private boolean setClient() {
        boolean res = false;
		Utils.appendLog("LCC " +  hotspotType.toString() + ": " + "setClient()");
		
		if(hotspotType == HotspotType.WIFI)
		{
			currentWifiHotspot = WifiAccessManager.connectToWifiAp(context, prefixNetworkSSID, "");
			res = currentWifiHotspot != null;
			
			if(res)
				Utils.appendLog("LCC " + hotspotType.toString() + ": " +
                        "Connected to hotspot " + hotspotType.toString() + " with SSID: " +
                        currentWifiHotspot.SSID);
		}
		else if(hotspotType == HotspotType.BLUETOOTH)
		{
			if(bt == null)	
				bt = new BluetoothTethering(context);
			
			currentBtHotspot = bt.startConnection(prefixNetworkSSID, "");
			res = currentBtHotspot != null;
			
			if(res)
				Utils.appendLog("LCC " + hotspotType.toString() + ": " +
                        "Connected to hotspot " + hotspotType.toString() + " with name: " +
                        currentBtHotspot.getName());
		}
		return res;
	}

	private boolean setHotspot() {
		boolean res = false;
		Utils.appendLog("LCC " + hotspotType.toString() + ": " +
                "setHotspot()");
		
		if(hotspotType == HotspotType.WIFI) {
			res = WifiAccessManager.setWifiApState(context, networkSSID, true);
			if(res)
				Utils.appendLog("LCC " + hotspotType.toString() + ": " +
                        "Activate hotspot " +
                        hotspotType.toString() + " with SSID " + networkSSID);
		}
		else if(hotspotType == HotspotType.BLUETOOTH) {
			if(bt == null)	
				bt = new BluetoothTethering(context);
			
			bt.setBluetoothTethering(true);
			res = bt.isBluetoothTetheringEnabled();
			
			if(res)
				Utils.appendLog("LCC " + hotspotType.toString() + ": " + "Activate hotspot " +
                        hotspotType.toString());
		}
		return res;
	}
	
	private boolean changeHotspot() {
		boolean res = false;
		Utils.appendLog("LCC " + hotspotType.toString() + ": " + "changeHotspot()");
		
		if(role == LCCRole.CLIENT) {
			//Client no change role, but can change hotspot
			if(hotspotType == HotspotType.WIFI) {
				ScanResult result = WifiAccessManager.connectToWifiAp(context, prefixNetworkSSID, "");
				
				//Test if i lost connection
				if(result == null) {
					currentWifiHotspot = null;
					Utils.appendLog("LCC " +  hotspotType.toString() + ": " +
                            "Lost connection! No hotspots found...");
				}
				
				if(result != null && currentWifiHotspot != null &&
                        result.SSID.equals(currentWifiHotspot.SSID)) {
					//I'm connect to the same hotspot wifi. Try to connect to a different hotspot
					result = WifiAccessManager.connectToWifiAp(context, prefixNetworkSSID, currentWifiHotspot.SSID);
				    if(result != null) {
				    	//Ok, client change hotspot
				    	currentWifiHotspot = result;
				    	res = true;
				    	Utils.appendLog("LCC " + hotspotType.toString() + ": " +
                                "Change hotspot " + hotspotType.toString() + ", connected to " +
                                currentWifiHotspot.SSID);
				    } else {
				    	//else, connected to the same hotspot
				    	Utils.appendLog("LCC " + hotspotType.toString() + ": " +
                                "Connected to the same hotspot " + hotspotType.toString());
				    }		    
				}
				
//				//Test if i lost connection
//				boolean connected = false;
//				if(currentWifiHotspot != null && WifiAccessManager.isWifiApConnect(context, currentWifiHotspot.SSID))
//					connected = true;
//				
//				if(connected)
//				{
//					//I'm connect to the same hotspot wifi. Try to connect to a different hotspot
//					ScanResult result = WifiAccessManager.connectToWifiAp(context, prefixNetworkSSID, currentWifiHotspot.SSID);
//				    if(result != null)
//				    {
//				    	//Ok, client change hotspot
//				    	currentWifiHotspot = result;
//				    	res = true;
//				    }
//				    //else, connected to the same hotspot
//				}
			} else if(hotspotType == HotspotType.BLUETOOTH) {
				if(bt == null)	
	    			bt = new BluetoothTethering(context);
				
//				BluetoothDevice result = bt.startConnection(prefixNetworkSSID, "");
//				
//				//Test if i lost connection
//				if(result == null)
//					currentBtHotspot = null;
//				
//				if(result != null && currentBtHotspot != null && 
//						result.getName().equals(currentBtHotspot.getName()))
//				{
//					//I'm connect to the same hotspot bluetooth. Try to connect to a different hotspot
//					result = bt.startConnection(prefixNetworkSSID, currentBtHotspot.getName());
//					if(result != null)
//				    {
//				    	//Ok, client change hotspot
//						currentBtHotspot = result;
//						res = true;
//				    }
//				    //else, connected to the same hotspot
//				}
				
//				if(currentBtHotspot != null)
//					currentBtHotspot = bt.startConnection(prefixNetworkSSID, currentBtHotspot.getName());
//				else
//					currentBtHotspot = bt.startConnection(prefixNetworkSSID, "");
//				if(currentBtHotspot != null)
//				{
//					    //Ok, client change hotspot
//					res = true;
//				}

				//Test if i lost connection
				boolean connected = false;
				if(currentBtHotspot != null) {
					if(bt.isConnectToDevice(currentBtHotspot))
					      connected = true;
					else
						currentBtHotspot = null;
				}
				
				if(connected) {
					//I'm connect to the same hotspot bluetooth. Try to connect to a different hotspot
					BluetoothDevice result = bt.startConnection(prefixNetworkSSID, currentBtHotspot.getName());
					if(result != null)
				    {
				    	//Ok, client change hotspot
						currentBtHotspot = result;
						res = true;
						Utils.appendLog("LCC " + hotspotType.toString() + ": " +
                                "Change hotspot " + hotspotType.toString() + ", connected to " +
                                currentBtHotspot.getName());
				    }
					else
					{	//else, connected to the same hotspot
				    	Utils.appendLog("LCC " + hotspotType.toString() + ": " +
                                "Connected to the same hotspot " + hotspotType.toString());
					}
				} else {
					Utils.appendLog("LCC " + hotspotType.toString() + ": " +
                            "Lost connection! No hotspots found...");
				}
			}
		}
        if (res)
            sendIntentBroadcast(Constants.MESSAGE_HOTSPOT_CHANGED);
		return res;
	}
	
	public void deactivate() {
		System.out.println("LCC " + hotspotType + ": DISABLED");
		Utils.appendLog("LCC " + hotspotType + ": DISABLED");

        removeNotification();

		this.active = false;
		interrupt();
	}
	
	public enum HotspotType {
	    WIFI, BLUETOOTH
	}
	
	public enum LCCRole {
	    CLIENT, HOTSPOT
	}

    /**
     * Show a notification while thread is running.
     */
    private void showNotification() {
        System.out.println("LCC " + hotspotType + ": showNotification");
        // The PendingIntent to launch our activity if the user selects this notification
        PendingIntent contentIntent = PendingIntent.getActivity(context, 0,
                new Intent(context, MainActivity.class), 0);
        // initialize the Notification
        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(context)
                        .setSmallIcon(R.drawable.ic_stat_wifi)
                        .setContentTitle("LCC")
                        .setTicker("LCC started")
                        .setContentIntent(contentIntent)
                        .setOngoing(true);
        switch (hotspotType) {
			case WIFI:
                notificationBuilder.setWhen(System.currentTimeMillis()).setContentText("WifiThread is active");
                // notifyID allows you to update the notification later on.
                notificationManager.notify(WIFI_ACTIVE_NOTIFICATION_ID, notificationBuilder.build());
                break;
			case BLUETOOTH:
                notificationBuilder.setWhen(System.currentTimeMillis()).setContentText("BluetoothThread is active");
                // notifyID allows you to update the notification later on.
                notificationManager.notify(BLUETOOTH_ACTIVE_NOTIFICATION_ID, notificationBuilder.build());
                break;
        }
    }


    private void removeNotification() {
        System.out.println("LCC " + hotspotType + ": removeNotification");
        switch (hotspotType) {
			case WIFI:
                notificationManager.cancel(WIFI_ACTIVE_NOTIFICATION_ID);
                break;
			case BLUETOOTH:
                notificationManager.cancel(BLUETOOTH_ACTIVE_NOTIFICATION_ID);
                break;
        }
    }


    private void sendIntentBroadcast(int message_id) {
        Intent intent = new Intent(Constants.WIFIOPP_INTENT_ACTION);
//        intent.setAction(Constants.WIFIOPP_INTENT_ACTION);
        intent.putExtra("data", message_id);
        context.sendBroadcast(intent);
    }

}
