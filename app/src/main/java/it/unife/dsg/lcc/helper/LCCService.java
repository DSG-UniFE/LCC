package it.unife.dsg.lcc.helper;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;

import it.unife.dsg.lcc.runtime.LCC;


public class LCCService extends Service {

    private LCC wifiThread = null;
    private LCC bluetoothThread = null;

    // Binder given to clients
    private final IBinder mBinder = new LCCBinder();

    /**
     * Class used for the client Binder.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    public class LCCBinder extends Binder {
        public LCCService getService() {
            // Return this instance of LocalService so clients can call public methods
            return LCCService.this;
        }
    }

    @Override
    public void onCreate() {
        System.out.println("LCCService: onCreate");
        // Start up the thread running the service.  Note that we create a
        // separate thread because the service normally runs in the process's
        // main thread, which we don't want to block.  We also make it
        // background priority so CPU-intensive work will not disrupt our UI.
        // The PendingIntent to launch our activity if the user selects this notification
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        System.out.println("LCCService: onStartCommand");
        //Toast.makeText(this, "service starting", Toast.LENGTH_SHORT).show();

        // If we get killed, after returning from here, restart
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        System.out.println("LCCService: onBind");
        return mBinder;
    }

    @Override
    public void onRebind(Intent intent) {
        System.out.println("LCCService: onRebind");
        super.onRebind(intent);
    }

    @Override
    public void onDestroy() {
        System.out.println("LCCService: onDestroy");
        super.onDestroy();
        stopWifiThread();
        stopBluetoothThread();
        //Toast.makeText(this, "service done", Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean onUnbind(Intent intent) {
        System.out.println("LCCService: onUnbind");
        return super.onUnbind(intent);
    }


    /** method for clients */
    public boolean wifiThreadIsActive() {
        if (wifiThread == null)
            return false;
        else
            return true;
    }

    public boolean bluetoothThreadIsActive() {
        if (bluetoothThread == null)
            return false;
        else
            return true;

    }

    public void startWifiThread(Context context, LCC.LCCRole role, int rs, int hc,
                                int maxTimewaitToBecomeHotspot, Handler uiHandler) {
        if (wifiThread == null) {
            wifiThread = new LCC(context, role, LCC.HotspotType.WIFI, rs, hc,
                    maxTimewaitToBecomeHotspot, uiHandler);
        }
    }

    public void stopWifiThread() {
        if (wifiThread != null) {
            wifiThread.deactivate();
            wifiThread = null;
        }

    }

    public void startBluetoothThread(Context context, LCC.LCCRole role, int rs, int hc,
                                     int maxTimewaitToBecomeHotspot, Handler uiHandler) {
        if (bluetoothThread == null) {
            bluetoothThread = new LCC(context, role, LCC.HotspotType.BLUETOOTH, rs, hc,
                    maxTimewaitToBecomeHotspot, uiHandler);
        }
    }

    public void stopBluetoothThread() {
        if (bluetoothThread != null) {
            bluetoothThread.deactivate();
            bluetoothThread = null;
        }
    }

}
