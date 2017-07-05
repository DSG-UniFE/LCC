package it.unife.dsg.lcc.configuration;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Set;

import android.content.Context;
import it.unife.dsg.lcc.util.Utils;
import android.bluetooth.*;
import android.widget.Toast;

/**
*
* @author Stefano Lanzone
*/

public class BluetoothTethering {

	private Object instance = null;
    private Method setTetheringOn = null;
    private Method isTetheringOn = null;
    private Method mBTPanConnect = null;
    private Method mBTDeviceConnState = null;
    private final Object mutex = new Object();
    private BluetoothAdapter mBluetoothAdapter;
    private BTPanServiceListener serviceListener;
    private Context context;
	
	public BluetoothTethering(Context context) {
        System.out.println("BluetoothTethering: BluetoothTethering()");

		try {
			Class<?> classBluetoothPan = Class.forName("android.bluetooth.BluetoothPan");
            Constructor<?> ctor = classBluetoothPan.getDeclaredConstructor(Context.class, BluetoothProfile.ServiceListener.class);
	        ctor.setAccessible(true);

            // Set Tethering ON
	        Class[] paramSet = new Class[1];
	        paramSet[0] = boolean.class;
	        
	        this.context = context;
	        mBluetoothAdapter = getBTAdapter();

            Class<?> noparams[] = {};
	        synchronized (mutex) {
	            setTetheringOn = classBluetoothPan.getDeclaredMethod("setBluetoothTethering", paramSet);
	            isTetheringOn = classBluetoothPan.getDeclaredMethod("isTetheringOn", noparams);
	            mBTPanConnect = classBluetoothPan.getDeclaredMethod("connect", BluetoothDevice.class);
	            mBTDeviceConnState = classBluetoothPan.getDeclaredMethod("getConnectionState", BluetoothDevice.class);
	            serviceListener = new BTPanServiceListener(context);
				instance = ctor.newInstance(context, serviceListener);
	        }

	    } catch (Exception e) {
	        e.printStackTrace();
	    }
	}

    private BluetoothAdapter getBTAdapter() {
        System.out.println("BluetoothTethering: getBTAdapter()");
        if (android.os.Build.VERSION.SDK_INT <= android.os.Build.VERSION_CODES.JELLY_BEAN_MR1)
            return BluetoothAdapter.getDefaultAdapter();
        else {
            BluetoothManager bm = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
            return bm.getAdapter();
        }
    }

    // Check whether Bluetooth tethering is enabled.
    public boolean isBluetoothTetheringEnabled() {
        System.out.println("BluetoothTethering: isBluetoothTetheringEnabled()");
        try {
            if(mBluetoothAdapter != null) {
//                Class<?> noparams[] = {};
//                return (Boolean) isTetheringOn.invoke(instance, (Object []) noparams);
                return (Boolean) isTetheringOn.invoke(instance, null);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    // Set Bluetooth tethering enabled/disabled.
    public void setBluetoothTethering(boolean on, String networkSSID) {
        System.out.println("BluetoothTethering: setBluetoothTethering()");
         try {
             if (on) {
                 setBluetooth(true);

                 if (mBluetoothAdapter != null) {
                     setName(networkSSID);
                     // setBluetooth(on);
                     // setTetheringOn.invoke(instance, on);
                 }
             } else {
                 setTetheringOn.invoke(instance, false);
             }
         } catch (Exception e) {
             e.printStackTrace();
         }
    }

    // Set Bluetooth enabled/disabled.
    private void setBluetooth(boolean on) {
        System.out.println("BluetoothTethering: setBluetooth()");
        try {
            if(mBluetoothAdapter != null) {
                if(on) {
                    if(!mBluetoothAdapter.isEnabled()) {
                        mBluetoothAdapter.enable();
                    }
                } else {
                    if(mBluetoothAdapter.isEnabled())
                        mBluetoothAdapter.disable();
                }
                Thread.sleep(1000);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

   private boolean setName(String name) {
       System.out.println("BluetoothTethering: setName()");
       boolean result = false;
       try {
           if(mBluetoothAdapter != null) {
               if (!mBluetoothAdapter.getName().equals(name)) {
                   mBluetoothAdapter.setName(name);
                   Thread.sleep(1000);
               }
               result = true;
           }
       } catch (Exception e) {
           e.printStackTrace();
       }
       return result;
   }

    public void restartBluetooth() {
        System.out.println("BluetoothTethering: restartBluetooth()");
        try {
            if(mBluetoothAdapter != null) {
                if(!mBluetoothAdapter.isEnabled()) {
                    mBluetoothAdapter.disable();
                    Thread.sleep(1000);
                    mBluetoothAdapter.enable();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

   public BluetoothDevice startConnection(String networkSSID, String excludeDevice) {
       System.out.println("BluetoothTethering: startConnection()");

       BluetoothDevice result = null;
       try {
           BluetoothProfile proxy = serviceListener.getBluetoothProfileProxy();
           setBluetooth(true);

           if(mBluetoothAdapter != null && proxy != null) {
               if(isBluetoothTetheringEnabled()) {
                   setTetheringOn.invoke(instance, false);
               }

               Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
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
                                   System.out.println("BluetoothTethering: Started connection");
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
       System.out.println("BluetoothTethering: isConnectToDevice()");

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
//    		   if(mBluetoothAdapter != null && proxy != null) {
//    			  
//    			   Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
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
	    private BluetoothProfile proxy = null;
	    
	    public BTPanServiceListener(final Context context) {
	        this.context = context;
	    }
	    
	    public BluetoothProfile getBluetoothProfileProxy() {
			return proxy;
	    }

	    @Override
	    public void onServiceConnected(final int profile, final BluetoothProfile proxy) {
            System.out.println("BTPanServiceListener, onServiceConnected()");
            this.proxy = proxy;
            
	        try {
	            synchronized (mutex) {
                    setBluetooth(true);
                    setTetheringOn.invoke(instance, true);
                    if ((Boolean)isTetheringOn.invoke(instance, null)) {
                        System.out.println("BTPanServiceListener, onServiceConnected: BT Tethering is on");
                        Toast.makeText(context, "BT Tethering is on", Toast.LENGTH_LONG).show();
                    }
                    else {
                        System.out.println("BTPanServiceListener, onServiceConnected: BT Tethering is off");
                        Toast.makeText(context, "BT Tethering is off", Toast.LENGTH_LONG).show();
                    }

	            }
	        } catch (InvocationTargetException e) {
                e.printStackTrace();
	        } catch (IllegalAccessException e) {
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
	            			System.out.println("BTPanServiceListener, onServiceDisconnected: BT Tethering is on");
//	            			Toast.makeText(getApplicationContext(), "BT Tethering is on", Toast.LENGTH_LONG).show();
	            		}
	            		else {
	            			System.out.println("BTPanServiceListener, onServiceDisconnected: BT Tethering is off");
//	                        Toast.makeText(getApplicationContext(), "BT Tethering is off", Toast.LENGTH_LONG).show();
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
