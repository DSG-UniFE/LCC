package it.unife.dsg.lcc.configuration;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Set;

import android.content.Context;
import android.net.ConnectivityManager;
import it.unife.dsg.lcc.util.Utils;
import android.bluetooth.*;

/**
*
* @author Stefano Lanzone
*/
public class BluetoothTethering {

	Object instance = null;
	Method setTetheringOn = null;
	Method isTetheringOn = null;
	Method mBTPanConnect = null;
	Method mBTDeviceConnState = null;
	Constructor<?> ctor;
	Object mutex = new Object();
	BluetoothAdapter btadapter;
	BTPanServiceListener serviceListener;
	Context context;
	
	public BluetoothTethering(Context context) {
		String sClassName = "android.bluetooth.BluetoothPan";
        
		try {
			Class<?> classBluetoothPan = Class.forName(sClassName);
	        ctor = classBluetoothPan.getDeclaredConstructor(Context.class, BluetoothProfile.ServiceListener.class);
	        ctor.setAccessible(true);

	        Class[] paramSet = new Class[1];
	        paramSet[0] = boolean.class;
	        Class<?> noparams[] = {};
	        
	        this.context = context;
	        btadapter = getBTAdapter();

	        synchronized (mutex) {
	            setTetheringOn = classBluetoothPan.getDeclaredMethod("setBluetoothTethering", paramSet);
	            isTetheringOn = classBluetoothPan.getDeclaredMethod("isTetheringOn", noparams);
	            mBTPanConnect = classBluetoothPan.getDeclaredMethod("connect", BluetoothDevice.class);
	            mBTDeviceConnState = classBluetoothPan.getDeclaredMethod("getConnectionState", BluetoothDevice.class);
	            serviceListener = new BTPanServiceListener(context);
				instance = ctor.newInstance(context, serviceListener);
	        }
	    } catch (ClassNotFoundException e) {
	        e.printStackTrace();
	    } catch (Exception e) {
	        e.printStackTrace();
	    }
	}

    private BluetoothAdapter getBTAdapter() {
        if (android.os.Build.VERSION.SDK_INT <= android.os.Build.VERSION_CODES.JELLY_BEAN_MR1)
            return BluetoothAdapter.getDefaultAdapter();
        else {
            BluetoothManager bm = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
            return bm.getAdapter();
        }
    }

    // Check whether Bluetooth tethering is enabled.
    public boolean isBluetoothTetheringEnabled() {
        try {
            if(btadapter != null) {
                Class<?> noparams[] = {};
                return (Boolean) isTetheringOn.invoke(instance, (Object []) noparams);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    // Set Bluetooth tethering enabled/disabled.
    public void setBluetoothTethering(boolean on, String networkSSID) {
         try {
            if(btadapter != null) {
                if (on)
                    setName(networkSSID);
                //setBluetooth(on);
                setTetheringOn.invoke(instance, on);
            }
         } catch (Exception e) {
             e.printStackTrace();
         }
    }

    // Set Bluetooth enabled/disabled.
    private void setBluetooth(boolean on) {
        try {
            if(btadapter != null) {
                if(on) {
                    if(!btadapter.isEnabled())
                        btadapter.enable();
                } else {
                    if(btadapter.isEnabled())
                        btadapter.disable();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

   public boolean setName(String name) {
       boolean result = false;
       try {
           if(btadapter != null) {
               System.out.println("BluetoothTethering: changed name");
               btadapter.setName(name);
               result = true;
           }
       } catch (Exception e) {
           e.printStackTrace();
       }
       return result;
   }

   public BluetoothDevice startConnection(String networkSSID, String excludeDevice) {
       BluetoothDevice result = null;
       try {
           BluetoothProfile proxy = serviceListener.getBluetoothProfileProxy();
           setBluetooth(true);

           if(btadapter != null && proxy != null) {
               if(isBluetoothTetheringEnabled()) {
                   setBluetoothTethering(false, "");
               }

               Set<BluetoothDevice> pairedDevices = btadapter.getBondedDevices();
               int size = pairedDevices.size();

               // If there are paired devices
               if (size > 0) {
                   ArrayList<BluetoothDevice> rampDevices = new ArrayList<BluetoothDevice>();
                   for (BluetoothDevice d : pairedDevices) {
                       String name = d.getName();
                       if(name.startsWith(networkSSID) && !name.equals(excludeDevice)) {
                           rampDevices.add(d);
                       }
                   }

                   size = rampDevices.size();
                   if (rampDevices.size() > 0) {
                       int n = Utils.nextRandomInt(size);
                       BluetoothDevice device = (BluetoothDevice) rampDevices.toArray()[n];
                       try {
                           String name = device.getName();
                           if (name.startsWith(networkSSID) && !name.equals(excludeDevice)) {
                               if (!((Boolean) mBTPanConnect.invoke(proxy, device))) {
                                   System.out.println("BluetoothTethering: Unable to start connection");
                               } else {
                                   System.out.println("BluetoothTethering: Start connection");
                                   result = device;
                               }
                           }
                       } catch (Exception e) {
                           e.printStackTrace();
                       }
                   } else {
                       System.out.println("BluetoothTethering: not found connection with filters");
                   }
               }
           }
        } catch (Exception e) {
            e.printStackTrace();
        }
       return result;
   }

   public boolean isConnectToDevice(BluetoothDevice device) {
       boolean result = false;
       try {
           BluetoothProfile proxy = serviceListener.getBluetoothProfileProxy();

           int state = (Integer)mBTDeviceConnState.invoke(proxy, device);

           if(state == BluetoothProfile.STATE_CONNECTED)
               result = true;
       } catch (Exception e) {
           e.printStackTrace();
       }
   return result;

   }
//       public boolean checkConnection(String deviceName)
//       {
//    	   boolean result = false;
//    	   try {
//    		   BluetoothProfile proxy = serviceListener.getBluetoothProfileProxy();
//    		   setBluetooth(true);
//    		   
//    		   if(btadapter != null && proxy != null) {
//    			  
//    			   Set<BluetoothDevice> pairedDevices = btadapter.getBondedDevices();
//		        	// If there are paired devices
//		        	if (pairedDevices.size() > 0) {
//		        		for (BluetoothDevice device : pairedDevices) {
//		        	        try{
//		        	        	String name = device.getName();
//		        	        	if(name.equals(deviceName))
//		        	        	{
//		        	        		if(!((Boolean) mBTPanConnect.invoke(proxy, device))){
//		        	        			//Log.e("MyApp", "Unable to start connection");
//		        	        			System.out.println("BluetoothTethering: Unable to start connection");
//		        	        		}
//		        	        		else
//		        	        		{
//		        	        			System.out.println("BluetoothTethering: Start connection");
//		        	        			result = true;
//		        	        			break;
//		        	        		}
//		        	        	}
//		        	        }
//		        	        catch (Exception e) {
//		        	            e.printStackTrace();
//		        	        }
//		        	    }
//		        	}
//    		   }	
//       		} catch (Exception e) {
//       			e.printStackTrace();
//       		}
//    	   return result;
//       }
	
	public class BTPanServiceListener implements BluetoothProfile.ServiceListener {

	    private final Context context;
	    private BluetoothProfile proxy;
	    
	    public BTPanServiceListener(final Context context) {
	        this.context = context;
	        this.proxy = null;
	    }
	    
	    public BluetoothProfile getBluetoothProfileProxy() {
			return proxy;
	    }

	    @Override
	    public void onServiceConnected(final int profile, final BluetoothProfile proxy) {
            this.proxy = proxy;
            
	        try {
	            synchronized (mutex) {
	            		setTetheringOn.invoke(instance, true);
	            		Class<?> noparams[] = {};
	            		if ((Boolean)isTetheringOn.invoke(instance, noparams)) {
	                	
	            			System.out.println("BluetoothTethering, onServiceConnected: BT Tethering is on");
	            			//Toast.makeText(getApplicationContext(), "BT Tethering is on", Toast.LENGTH_LONG).show();
	            		}
	            		else {
	            			System.out.println("BluetoothTethering, onServiceConnected: BT Tethering is off");
	            			//Toast.makeText(getApplicationContext(), "BT Tethering is off", Toast.LENGTH_LONG).show();
	            		}
//	            	}
//	            	else
//	            	{
//	                	if(btadapter != null) {
////	    		        	setBluetooth(true);
//	    		        	Set<BluetoothDevice> pairedDevices = btadapter.getBondedDevices();
//	    		        	boolean isConnect = false;
//	    		        	// If there are paired devices
//	    		        	if (pairedDevices.size() > 0) {
//	    		        	    // Loop through paired devices
//	    		        	    for (BluetoothDevice device : pairedDevices) {
//	    		        	        try{
//	    		        	        	String name = device.getName();
////	    		        	        	if(name.equals("Galaxy S5"))
////	    		        	        	{	
//	    		        	        		if(!((Boolean) mBTPanConnect.invoke(proxy, device))){
//	    		        	                    //Log.e("MyApp", "Unable to start connection");
//	    		        	        			System.out.println("BluetoothTethering: Unable to start connection");
//	    		        	                }
//	    		        	        		else
//	    		        	        		{
//	    		        	        			System.out.println("BluetoothTethering: Start connection");
//	    		        	        			isConnect = true;
//	    		        	        			break;
//	    		        	        		}
////	    		        	        	}	    		        	        
//	    		        	        }
//	    		        	        catch (Exception e) {
//	    		        	            e.printStackTrace();
//	    		        	        }
//	    		        	    }
//	    		        	}
//	    		        }
//	                }
	            }
	        }
	        catch (InvocationTargetException e) {
                e.printStackTrace();
	        }
	        catch (IllegalAccessException e) {
                e.printStackTrace();
	        }
	    }

	    @Override
	    public void onServiceDisconnected(final int profile) {
	    	try {
	            synchronized (mutex) {
//	            	if(!connectToHotspot) {
	            		setTetheringOn.invoke(instance, false);
	            		if ((Boolean)isTetheringOn.invoke(instance, null)) {
	                	
	            			System.out.println("BluetoothTethering, onServiceDisconnected: BT Tethering is on");
	            			//Toast.makeText(getApplicationContext(), "BT Tethering is on", Toast.LENGTH_LONG).show();
	            		}
	            		else {
	            			System.out.println("BluetoothTethering, onServiceDisconnected: BT Tethering is off");
	                        //Toast.makeText(getApplicationContext(), "BT Tethering is off", Toast.LENGTH_LONG).show();
	            		}
//	            	}
	            }
	        }
	        catch (InvocationTargetException e) {
	            e.printStackTrace();
	        }
	        catch (IllegalAccessException e) {
	            e.printStackTrace();
	        }
	    }
	}
	
}
