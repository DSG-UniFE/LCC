package it.unife.dsg.lcc;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.net.Uri;
import android.net.wifi.ScanResult;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.CompoundButton.OnCheckedChangeListener;

import it.unife.dsg.lcc.configuration.BluetoothTethering;
import it.unife.dsg.lcc.configuration.WifiAccessManager;
import it.unife.dsg.lcc.helper.LCCService;
import it.unife.dsg.lcc.runtime.LCC.LCCRole;
import it.unife.dsg.lcc.util.Constants;
import it.unife.dsg.lcc.util.Utils;

import android.widget.Spinner;
import android.widget.Toast;

/**
 *
 * @author Stefano Lanzone
 */

public class MainActivity extends AppCompatActivity implements OnCheckedChangeListener, OnItemSelectedListener {

    private LCCService mService;
    private boolean mBound = false;

	private Handler uiHandler;

    CheckBox LCCThread, wifiHotspotRule;
    CheckBox lccThreadBluetooth, bluetoothHotspotRole;
    CheckBox wifiHotspotActive, bluetoothHotspotActive;
    CheckBox wifiHotspotConnect, bluetoothHotspotConnect;
    Context context;
    String networkSSID, prefixNetworkSSID;
    BluetoothTethering bt;
    LCCRole roleWifi, roleBluetooth;

    private final int STATUS_READY_TO_START = 0;
    private final int STATUS_DEFAULT_VALUES = 2;
    private final int STATUS_LAZY_VALUE = 3;
    private final int STATUS_REGULAR_VALUE = 4;
    private final int STATUS_HECTIC_VALUE = 5;
    private final int STATUS_ENABLE_EDIT_TEXT = 6;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        System.out.println("MainActivity: onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        context = getApplicationContext();
        prefixNetworkSSID = "RAMP hotspot ";
        networkSSID = prefixNetworkSSID + Utils.nextRandomInt();
        bt = null;
        roleWifi = LCCRole.CLIENT;
        roleBluetooth = LCCRole.CLIENT;

		updateUI(STATUS_READY_TO_START);

        // hide soft keyboard at activity start-up
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.options_menu, menu);
        return true;
    }


    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        final int id = buttonView.getId();
        switch (id) {
            case R.id.wifiHotspotRole:
                System.out.println("LCCActivity: onCheckedChanged = R.id.wifiHotspotRole " + isChecked);
                Utils.appendLog("LCCActivity: onCheckedChanged = R.id.wifiHotspotRole " + isChecked);

                if (!isChecked)
                    roleWifi = LCCRole.CLIENT;
                else
                    roleWifi = LCCRole.HOTSPOT;
                break;
            case R.id.lccThread:
                System.out.println("LCCActivity: onCheckedChanged = R.id.lccThread " + isChecked);
                Utils.appendLog("LCCActivity: onCheckedChanged = R.id.lccThread " + isChecked);

                if (mBound) {
                    if (!mService.wifiThreadIsActive()) {
                        //lcc = LCC.getInstance(true, context, roleWifi, HotspotType.WIFI);
                        int rs = Integer.parseInt(((EditText) findViewById(R.id.changeRolePeriodValue)).getText().toString());
                        int hc = Integer.parseInt(((EditText) findViewById(R.id.changeHotspotPeriodValue)).getText().toString());
                        int maxtbh = Integer.parseInt(((EditText) findViewById(R.id.maxTimewaitToBeHotspotValue)).getText().toString());

                        mService.startWifiThread(context, roleWifi, rs, hc, maxtbh);
                    }
                    if (!isChecked) {
                        mService.stopWifiThread();
                    }
                }
                break;
            case R.id.bluetoothHotspotRole:
                System.out.println("LCCActivity: onCheckedChanged = R.id.bluetoothHotspotRole " + isChecked);
                Utils.appendLog("LCCActivity: onCheckedChanged = R.id.bluetoothHotspotRole " + isChecked);

                if (!isChecked)
                    roleBluetooth = LCCRole.CLIENT;
                else
                    roleBluetooth = LCCRole.HOTSPOT;
                break;
            case R.id.lccThreadBluetooth:
                System.out.println("LCCActivity: onCheckedChanged = R.id.lccThreadBluetooth " + isChecked);
                Utils.appendLog("LCCActivity: onCheckedChanged = R.id.lccThreadBluetooth " + isChecked);

                if (mBound) {
                    if (!mService.bluetoothThreadIsActive()) {
                        //lccBt = LCC.getInstance(true, context, roleBluetooth, HotspotType.BLUETOOTH);
                        int rs = Integer.parseInt(((EditText) findViewById(R.id.changeRolePeriodValue)).getText().toString());
                        int hc = Integer.parseInt(((EditText) findViewById(R.id.changeHotspotPeriodValue)).getText().toString());
                        int maxtbh = Integer.parseInt(((EditText) findViewById(R.id.maxTimewaitToBeHotspotValue)).getText().toString());

                        mService.startBluetoothThread(context, roleBluetooth, rs, hc, maxtbh);
                    }
                    if (!isChecked) {
                        mService.stopBluetoothThread();
                    }
                }
                break;
            case R.id.hotspotWifiActive:
                System.out.println("LCCActivity: onCheckedChanged = R.id.hotspotWifiActive " + isChecked);
                Utils.appendLog("LCCActivity: onCheckedChanged = R.id.hotspotWifiActive " + isChecked);

                if (isChecked) {
                    boolean res = WifiAccessManager.setWifiApState(context, networkSSID, true);
                    if (res) {
                        Toast.makeText(context, "Hotspot Wifi activate", Toast.LENGTH_LONG).show();
                        Utils.appendLog("LCCActivity: Hotspot Wifi activate");
                    } else {
                        Toast.makeText(context, "Hotspot Wifi fail activatation!", Toast.LENGTH_LONG).show();
                        Utils.appendLog("LCCActivity: Hotspot Wifi fail activatation!");
                        wifiHotspotActive.setChecked(false);
                    }
                } else {
                    boolean res = WifiAccessManager.setWifiApState(context, networkSSID, false);
                    if (res) {
                        Toast.makeText(context, "Hotspot Wifi deactivate", Toast.LENGTH_LONG).show();
                        Utils.appendLog("LCCActivity: Hotspot Wifi deactivate");
                    } else {
                        Toast.makeText(context, "Hotspot Wifi fail deactivatation!", Toast.LENGTH_LONG).show();
                        wifiHotspotActive.setChecked(true);
                        Utils.appendLog("LCCActivity: Hotspot Wifi fail deactivatation!");
                    }
                }
                break;
            case R.id.hotspotBluetoothActive:
                //Bluetooth server
                System.out.println("LCCActivity: onCheckedChanged = R.id.hotspotBluetoothActive " + isChecked);
                Utils.appendLog("LCCActivity: onCheckedChanged = R.id.hotspotBluetoothActive " + isChecked);

                //TODO da controllare (perchè manca il ramo else)
                if (isChecked) {
                    if (bt == null)
                        bt = new BluetoothTethering(context);

                    bt.setBluetoothTethering(true);
                    boolean res = bt.isBluetoothTetheringEnabled();
                    bluetoothHotspotActive.setChecked(res);
                    if (res) {
                        Toast.makeText(context, "Hotspot Bluetooth enabled", Toast.LENGTH_LONG).show();
                        Utils.appendLog("LCCActivity: Hotspot Bluetooth enabled");
                    } else {
                        Toast.makeText(context, "Hotspot Bluetooth disabled!", Toast.LENGTH_LONG).show();
                        Utils.appendLog("LCCActivity: Hotspot Bluetooth disabled!");
                    }
                } else {
                    //TODO da fare
                }
                break;
            case R.id.hotspotWifiConnect:
                System.out.println("LCCActivity: onCheckedChanged = R.id.hotspotWifiConnect " + isChecked);
                Utils.appendLog("LCCActivity: onCheckedChanged = R.id.hotspotWifiConnect " + isChecked);

                if (isChecked) {
                    ScanResult res = WifiAccessManager.connectToWifiAp(context, prefixNetworkSSID, "");
                    if (res != null) {
                        Toast.makeText(context, "Hotspot Wifi connect", Toast.LENGTH_LONG).show();
                        Utils.appendLog("LCCActivity: Hotspot Wifi connect");
                    } else {
                        Toast.makeText(context, "Hotspot Wifi fail activatation!", Toast.LENGTH_LONG).show();
                        wifiHotspotConnect.setChecked(false);
                        Utils.appendLog("LCCActivity: Hotspot Wifi fail activatation!");
                    }
                } else {
                    //boolean res = WifiAccessManager.connectToWifiAp(context, prefixNetworkSSID, false);
                    //if(res)
                    //    Toast.makeText(context, "Hotspot Wifi deactivate ", Toast.LENGTH_LONG).show();
                    //else {
                    //    Toast.makeText(context, "Hotspot Wifi fail deactivatation!", Toast.LENGTH_LONG).show();
                    //    wifiHotspotConnect.setChecked(true);
                    //}
                }
                break;
            case R.id.hotspotBluetoothConnect:
                System.out.println("LCCActivity: onCheckedChanged = R.id.hotspotBluetoothConnect " + isChecked);
                Utils.appendLog("LCCActivity: onCheckedChanged = R.id.hotspotBluetoothConnect " + isChecked);

                if (isChecked) {
                    if (bt == null)
                        bt = new BluetoothTethering(context);

                    BluetoothDevice res = bt.startConnection(prefixNetworkSSID, "");
                    if (res != null) {
                        Toast.makeText(context, "Hotspot Bluetooth: Start connection", Toast.LENGTH_LONG).show();
                        Utils.appendLog("LCCActivity: Hotspot Bluetooth start connection");
                    } else {
                        Toast.makeText(context, "Hotspot Bluetooth: Unable to start connection", Toast.LENGTH_LONG).show();
                        Utils.appendLog("LCCActivity: Hotspot Bluetooth unable to start connection");
                    }
                }

                //bt.setBluetoothTethering(false);
                break;
        }
    }


    public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
        // An item was selected. You can retrieve the selected item using
        // parent.getItemAtPosition(pos)
        //startup = false;
        switch (pos) {
            case 0:
                updateUI(STATUS_LAZY_VALUE);
                break;
            case 1:
                updateUI(STATUS_REGULAR_VALUE);
                break;
            case 2:
                updateUI(STATUS_HECTIC_VALUE);
                break;
            case 3:
                updateUI(STATUS_ENABLE_EDIT_TEXT);
                break;
        }
    }


    public void onNothingSelected(AdapterView<?> parent) {
        // Another interface callback
        Toast.makeText(context, "onNothingSelected", Toast.LENGTH_LONG).show();
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.default_values:
                updateUI(STATUS_DEFAULT_VALUES);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }


    private void updateUI(int status) {
        switch (status) {
            case STATUS_READY_TO_START:
                //Aggressiveness
                Spinner aggressiveness_spinner = (Spinner) findViewById(R.id.aggressiveness_spinner);
                aggressiveness_spinner.setOnItemSelectedListener(this);
                /*
                ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                        R.array.aggressiveness_array, android.R.layout.simple_spinner_item);
                // Specify the layout to use when the list of choices appears
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                // Apply the adapter to the spinner
                aggressiveness_spinner.setAdapter(adapter);
                */
                aggressiveness_spinner.setSelection(1);

                //Hotspot Wifi
                wifiHotspotRule = (CheckBox)findViewById(R.id.wifiHotspotRole);
                wifiHotspotRule.setOnCheckedChangeListener(this);
                LCCThread = (CheckBox)findViewById(R.id.lccThread);
                LCCThread.setOnCheckedChangeListener(this);
                //Hotspot Bluetooth
                bluetoothHotspotRole = (CheckBox)findViewById(R.id.bluetoothHotspotRole);
                bluetoothHotspotRole.setOnCheckedChangeListener(this);
                lccThreadBluetooth = (CheckBox)findViewById(R.id.lccThreadBluetooth);
                lccThreadBluetooth.setOnCheckedChangeListener(this);

                //Server
                wifiHotspotActive = (CheckBox)findViewById(R.id.hotspotWifiActive);
                wifiHotspotActive.setOnCheckedChangeListener(this);
                bluetoothHotspotActive = (CheckBox)findViewById(R.id.hotspotBluetoothActive);
                bluetoothHotspotActive.setOnCheckedChangeListener(this);
                //Client
                wifiHotspotConnect = (CheckBox)findViewById(R.id.hotspotWifiConnect);
                wifiHotspotConnect.setOnCheckedChangeListener(this);
                bluetoothHotspotConnect = (CheckBox)findViewById(R.id.hotspotBluetoothConnect);
                bluetoothHotspotConnect.setOnCheckedChangeListener(this);

                break;
            case STATUS_DEFAULT_VALUES:
                //startup = true;
                ((Spinner) findViewById(R.id.aggressiveness_spinner)).setSelection(1);

                ((CheckBox)findViewById(R.id.wifiHotspotRole)).setChecked(false);
                ((CheckBox)findViewById(R.id.lccThread)).setChecked(false);

                ((CheckBox)findViewById(R.id.bluetoothHotspotRole)).setChecked(false);
                ((CheckBox)findViewById(R.id.lccThreadBluetooth)).setChecked(false);

                if (((CheckBox)findViewById(R.id.hotspotWifiActive)).isChecked())
                    WifiAccessManager.setWifiApState(context, networkSSID, false);
                ((CheckBox)findViewById(R.id.hotspotWifiActive)).setChecked(false);

                ((CheckBox)findViewById(R.id.hotspotBluetoothActive)).setChecked(false);
                ((CheckBox)findViewById(R.id.hotspotWifiConnect)).setChecked(false);
                ((CheckBox)findViewById(R.id.hotspotBluetoothConnect)).setChecked(false);

                break;
            case STATUS_LAZY_VALUE:
                (findViewById(R.id.changeRolePeriodValue)).setEnabled(false);
                ((EditText) findViewById(R.id.changeRolePeriodValue))
                        .setText(String.valueOf(Constants.DEFAULT_LAZY_RS));

                (findViewById(R.id.changeHotspotPeriodValue)).setEnabled(false);
                ((EditText) findViewById(R.id.changeHotspotPeriodValue))
                        .setText(String.valueOf(Constants.DEFAULT_LAZY_HC));

                (findViewById(R.id.maxTimewaitToBeHotspotValue)).setEnabled(false);
                ((EditText) findViewById(R.id.maxTimewaitToBeHotspotValue))
                        .setText(String.valueOf(Constants.DEFAULT_LAZY_MAXTBH));
                break;
            case STATUS_REGULAR_VALUE:
                (findViewById(R.id.changeRolePeriodValue)).setEnabled(false);
                ((EditText) findViewById(R.id.changeRolePeriodValue))
                        .setText(String.valueOf(Constants.DEFAULT_REGULAR_RS));

                (findViewById(R.id.changeHotspotPeriodValue)).setEnabled(false);
                ((EditText) findViewById(R.id.changeHotspotPeriodValue))
                        .setText(String.valueOf(Constants.DEFAULT_REGULAR_HC));

                (findViewById(R.id.maxTimewaitToBeHotspotValue)).setEnabled(false);
                ((EditText) findViewById(R.id.maxTimewaitToBeHotspotValue))
                        .setText(String.valueOf(Constants.DEFAULT_REGULAR_MAXTBH));
                break;
            case STATUS_HECTIC_VALUE:
                (findViewById(R.id.changeRolePeriodValue)).setEnabled(false);
                ((EditText) findViewById(R.id.changeRolePeriodValue))
                        .setText(String.valueOf(Constants.DEFAULT_HECTIC_RS));

                (findViewById(R.id.changeHotspotPeriodValue)).setEnabled(false);
                ((EditText) findViewById(R.id.changeHotspotPeriodValue))
                        .setText(String.valueOf(Constants.DEFAULT_HECTIC_HC));

                (findViewById(R.id.maxTimewaitToBeHotspotValue)).setEnabled(false);
                ((EditText) findViewById(R.id.maxTimewaitToBeHotspotValue))
                        .setText(String.valueOf(Constants.DEFAULT_HECTIC_MAXTBH));
                break;
            case STATUS_ENABLE_EDIT_TEXT:
                EditText changeRolePeriod = (EditText) findViewById(R.id.changeRolePeriodValue);
                changeRolePeriod.setEnabled(true);
                //changeRolePeriod.setText(String.valueOf(Constants.DEFAULT_LAZY_RS));

                EditText changeHotspotPeriod = (EditText) findViewById(R.id.changeHotspotPeriodValue);
                changeHotspotPeriod.setEnabled(true);
                //changeHotspotPeriod.setText(String.valueOf(Constants.DEFAULT_LAZY_HC));

                EditText maxTimewaitToBeHotspot = (EditText) findViewById(R.id.maxTimewaitToBeHotspotValue);
                maxTimewaitToBeHotspot.setEnabled(true);
                //maxTimewaitToBeHotspot.setText(String.valueOf(Constants.DEFAULT_LAZY_MAXTBH));

                break;
        }
    }


    private void restoreActivityState(){
    	System.out.println("MainActivity: restoreActivityState");
		// Use shared preferences to restore the activity state
		SharedPreferences settings = getPreferences(MODE_PRIVATE);

        int aggressiveness = 1;
        int rs = Constants.DEFAULT_REGULAR_RS;
		int hc = Constants.DEFAULT_REGULAR_HC;
		int maxtbh = Constants.DEFAULT_REGULAR_MAXTBH;
        boolean wifi_hotspot_role = false;
        boolean lcc_thread = false;
        boolean bluetooth_hotspot_role = false;
        boolean lcc_thread_bluetooth = false;

        aggressiveness = settings.getInt("aggressiveness", aggressiveness);
		rs = settings.getInt("rs", rs);
		hc = settings.getInt("hc", hc);
		maxtbh = settings.getInt("maxtbh", maxtbh);
        wifi_hotspot_role = settings.getBoolean("wifi_hotspot_role", wifi_hotspot_role);
        lcc_thread = settings.getBoolean("lcct_thread", lcc_thread);
        bluetooth_hotspot_role = settings.getBoolean("bluetooth_hotspot_role", bluetooth_hotspot_role);
        lcc_thread_bluetooth = settings.getBoolean("lcc_thread_bluetooth", lcc_thread_bluetooth);

        ((Spinner) findViewById(R.id.aggressiveness_spinner)).setSelection(aggressiveness);
		((EditText)findViewById(R.id.changeRolePeriodValue)).setText(String.valueOf(rs));
		((EditText)findViewById(R.id.changeHotspotPeriodValue)).setText(String.valueOf(hc));
		((EditText)findViewById(R.id.maxTimewaitToBeHotspotValue)).setText(String.valueOf(maxtbh));
        ((CheckBox)findViewById(R.id.wifiHotspotRole)).setChecked(wifi_hotspot_role);
        ((CheckBox)findViewById(R.id.lccThread)).setChecked(lcc_thread);
        ((CheckBox)findViewById(R.id.bluetoothHotspotRole)).setChecked(bluetooth_hotspot_role);
        ((CheckBox)findViewById(R.id.lccThreadBluetooth)).setChecked(lcc_thread_bluetooth);
    }


    private void saveActivityState(){
		System.out.println("MainActivity: saveActivityState");
		// Use shared preferences to save the activity state
		SharedPreferences settings = getPreferences(MODE_PRIVATE);
		SharedPreferences.Editor editor = settings.edit();
		editor.clear();
		
		// save parameters
        int aggressiveness = (int) ((Spinner) findViewById(R.id.aggressiveness_spinner)).getSelectedItemId();
		int rs = Integer.parseInt(((EditText)findViewById(R.id.changeRolePeriodValue)).getText().toString());
		int hc = Integer.parseInt(((EditText)findViewById(R.id.changeHotspotPeriodValue)).getText().toString());
		int maxtbh = Integer.parseInt(((EditText)findViewById(R.id.maxTimewaitToBeHotspotValue)).getText().toString());

        boolean wifi_hotspot_role = ((CheckBox)findViewById(R.id.wifiHotspotRole)).isChecked();
        boolean lcct_thread = ((CheckBox)findViewById(R.id.lccThread)).isChecked();
        boolean bluetooth_hotspot_role = ((CheckBox)findViewById(R.id.bluetoothHotspotRole)).isChecked();
        boolean lcc_thread_bluetooth = ((CheckBox)findViewById(R.id.lccThreadBluetooth)).isChecked();

        editor.putInt("aggressiveness", aggressiveness);
        editor.putInt("rs", rs);
		editor.putInt("hc", hc);
		editor.putInt("maxtbh", maxtbh);
        editor.putBoolean("wifi_hotspot_role", wifi_hotspot_role);
        editor.putBoolean("lcct_thread", lcct_thread);
        editor.putBoolean("bluetooth_hotspot_role", bluetooth_hotspot_role);
        editor.putBoolean("lcc_thread_bluetooth", lcc_thread_bluetooth);

		// Commit
		editor.apply();
	}

//    @Override
//    public void onSaveInstanceState(Bundle outState) {
//        System.out.println("###->onSaveInstanceState");
//        int aggressiveness = (int) ((Spinner) findViewById(R.id.aggressiveness_spinner)).getSelectedItemId();
//        int rs = Integer.parseInt(((EditText)findViewById(R.id.changeRolePeriodValue)).getText().toString());
//        int hc = Integer.parseInt(((EditText)findViewById(R.id.changeHotspotPeriodValue)).getText().toString());
//        int maxtbh = Integer.parseInt(((EditText)findViewById(R.id.maxTimewaitToBeHotspotValue)).getText().toString());
//
//        outState.putInt("aggressiveness", aggressiveness);
//        outState.putInt("rs", rs);
//        outState.putInt("hc", hc);
//        outState.putInt("maxtbh", maxtbh);
//
//        // Always call the superclass so it can save the view hierarchy state
//        super.onSaveInstanceState(outState);
//    }

//    @Override
//    public void onRestoreInstanceState(Bundle savedInstanceState) {
//        System.out.println("###->onRestoreInstanceState");
//        // Always call the superclass so it can restore the view hierarchy
//        super.onRestoreInstanceState(savedInstanceState);
//
//        int aggressiveness = 1;
//        int rs = Constants.DEFAULT_REGULAR_RS;
//        int hc = Constants.DEFAULT_REGULAR_HC;
//        int maxtbh = Constants.DEFAULT_REGULAR_MAXTBH;
//
//        aggressiveness = savedInstanceState.getInt("aggressiveness", aggressiveness);
//        rs = savedInstanceState.getInt("rs", rs);
//        hc = savedInstanceState.getInt("hc", hc);
//        maxtbh = savedInstanceState.getInt("maxtbh", maxtbh);
//
//        ((Spinner) findViewById(R.id.aggressiveness_spinner)).setSelection(aggressiveness);
//        ((EditText)findViewById(R.id.changeRolePeriodValue)).setText(String.valueOf(rs));
//        ((EditText)findViewById(R.id.changeHotspotPeriodValue)).setText(String.valueOf(hc));
//        ((EditText)findViewById(R.id.maxTimewaitToBeHotspotValue)).setText(String.valueOf(maxtbh));
//    }

    @Override
    protected void onStart() {
        System.out.println("MainActivity: onStart");
        super.onStart();
        writeSettingsPermission(this);
        if (!mBound) {
            // Bind to LocalService
            Intent intent = new Intent(this, LCCService.class);
            bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
        }

        // TODO solo per test
        if (mBound) {
            System.out.println("--->mService.wifiThreadIsActive(): " + mService.wifiThreadIsActive());
            System.out.println("--->mService.bluetoothThreadIsActive(): " + mService.bluetoothThreadIsActive());
        } else {
            System.out.println("--->RAMO ELSE");
        }
    }

    @Override
    protected void onPause() {
        System.out.println("MainActivity: onPause");
        super.onPause();
        saveActivityState();
    }

    @Override
    protected void onRestart() {
        System.out.println("MainActivity: onRestart");
        super.onRestart();
    }

    @Override
    protected void onResume() {
        System.out.println("MainActivity: onResume");
        super.onResume();
        restoreActivityState();
    }

    @Override
    protected void onStop() {
        System.out.println("MainActivity: onStop");
        super.onStop();

        // Unbind from the service
        if (!mService.wifiThreadIsActive() && !mService.bluetoothThreadIsActive()) {
            unbindService(mConnection);
            mBound = false;
        }
    }

    private static class UIHandler extends Handler {
    	// this handler is used to popup messages, 
    	// sent from other threads, as the main app thread 
    	// (otherwise RuntimeException)
    	
    	private static final int DISPLAY_UI_TOAST = 0;
    	private Context context;

        public UIHandler(Looper looper, Context context){
            super(looper);
            this.context = context;
        }

        @Override
        public void handleMessage(Message msg){
            switch(msg.what){
            case UIHandler.DISPLAY_UI_TOAST:
            	Toast.makeText(context, (String) msg.obj, Toast.LENGTH_LONG).show();
                break;
            default:
                break;
            }
        }
    }

    /** Defines callbacks for service binding, passed to bindService() */
    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            // We've bound to LCCService, cast the IBinder and get LCCService instance
            System.out.println("MainActivity: onServiceConnected");
            LCCService.LCCBinder binder = (LCCService.LCCBinder) service;
            mService = binder.getService();
            mBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName className) {
            // This is called when the connection with the service has been
            // unexpectedly disconnected -- that is, its process crashed.
            System.out.println("MainActivity: onServiceDisconnected");
            updateUI(STATUS_DEFAULT_VALUES);
            mService = null;
            mBound = false;
        }
    };

    public static void writeSettingsPermission(Activity context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.System.canWrite(context)) {
                Intent intent = new Intent(android.provider.Settings.ACTION_MANAGE_WRITE_SETTINGS);
                intent.setData(Uri.parse("package:" + context.getPackageName()));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
            }
        }
    }

}
