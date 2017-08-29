package it.unife.dsg.lcc;

import android.Manifest;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.provider.Settings;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.text.Spanned;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import it.unife.dsg.lcc.helper.LCCService;
import it.unife.dsg.lcc.runtime.LCC.LCCRole;
import it.unife.dsg.lcc.util.Constants;


public class MainActivity extends AppCompatActivity implements OnCheckedChangeListener, OnItemSelectedListener {

    private LCCService mService;
    private boolean mBound = false;

	private Handler uiHandler;
//    private UIHandlerThread uiHandlerThread;

    private Context context;
    private LCCRole roleWifi, roleBluetooth;
    private TextView networkIDTextView, updatedTextView;
    private TextView wifiRoleTextView, wifiConnectedToTextView, wifiIpTextView;
    private TextView bluetoothRoleTextView, bluetoothConnectedToTextView, bluetoothIpTextView;

    private final int STATUS_READY_TO_START = 0;
    private final int STATUS_DEFAULT_VALUES = 2;
    private final int STATUS_LAZY_VALUE = 3;
    private final int STATUS_REGULAR_VALUE = 4;
    private final int STATUS_HECTIC_VALUE = 5;
    private final int STATUS_ENABLE_EDIT_TEXT = 6;

    final private int REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS = 135;


    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        System.out.println("MainActivity: onCreate()");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        context = getApplicationContext();

        roleWifi = LCCRole.CLIENT;
        roleBluetooth = LCCRole.CLIENT;

        // Setup the handler for UI interaction from other threads
//        HandlerThread uiThread = new HandlerThread("UIHandler");
//        uiThread.start();
//        uiHandler = new UIHandler(uiThread.getLooper(), getApplicationContext());

//        uiHandlerThread = new UIHandlerThread("UIHandlerThread");
//        uiHandlerThread.start();

        uiHandler = new Handler(Looper.getMainLooper()) {
            private static final int UPDATE_UI_WIFI = 1;
            private static final int UPDATE_UI_BLUETOOTH = 2;

            /*
            * handleMessage() defines the operations to perform when
            * the Handler receives a new Message to process.
            */
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case UPDATE_UI_WIFI:
                        try {
                            HashMap<String, String> info = (HashMap<String, String>) msg.obj;

                            networkIDTextView = (TextView) findViewById(R.id.network_id);
                            Spanned text = fromHtml("<b>" + getString(R.string.network_id) + "</b> " +
                                    info.get("network_id"));
                            networkIDTextView.setText(text);
                            updatedTextView = (TextView) findViewById(R.id.updated);
                            text = fromHtml("<b>" + getString(R.string.updated) + "</b> " +
                                    info.get("updated"));
                            updatedTextView.setText(text);

                            wifiRoleTextView = (TextView) findViewById(R.id.wifi_role);
                            text = fromHtml("<b>" + getString(R.string.role) + "</b> " +
                                    info.get("wifi_role"));
                            wifiRoleTextView.setText(text);
                            wifiIpTextView = (TextView) findViewById(R.id.wifi_ip);
                            text = fromHtml("<b>" + getString(R.string.ip) + "</b> " +
                                    info.get("wifi_ip"));
                            wifiIpTextView.setText(text);
                            wifiConnectedToTextView = (TextView) findViewById(R.id.wifi_connect_to);
                            text = fromHtml("<b>" + getString(R.string.connected_to) + "</b> " +
                                    info.get("wifi_connected_to"));
                            wifiConnectedToTextView.setText(text);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        break;

                    case UPDATE_UI_BLUETOOTH:
                        try {
                            HashMap<String, String> info = (HashMap<String, String>) msg.obj;

                            networkIDTextView = (TextView) findViewById(R.id.network_id);
                            Spanned text = fromHtml("<b>" + getString(R.string.network_id) +
                                    "</b> " + info.get("network_id"));
                            networkIDTextView.setText(text);
                            updatedTextView = (TextView) findViewById(R.id.updated);
                            text = fromHtml("<b>" + getString(R.string.updated) + "</b> " +
                                    info.get("updated"));
                            updatedTextView.setText(text);

                            bluetoothRoleTextView = (TextView) findViewById(R.id.bluetooth_role);
                            text = fromHtml("<b>" + getString(R.string.role) + "</b> " +
                                    info.get("bluetooth_role"));
                            bluetoothRoleTextView.setText(text);
                            bluetoothIpTextView = (TextView) findViewById(R.id.bluetooth_ip);
                            text = fromHtml("<b>" + getString(R.string.ip) + "</b> " +
                                    info.get("bluetooth_ip"));
                            bluetoothIpTextView.setText(text);
                            bluetoothConnectedToTextView = (TextView) findViewById(R.id.bluetooth_connect_to);
                            text = fromHtml("<b>" + getString(R.string.connected_to) + "</b> " +
                                    info.get("bluetooth_connected_to"));
                            bluetoothConnectedToTextView.setText(text);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        break;

                    default:
                        HashMap<String, String> info = (HashMap<String, String>) msg.obj;

                        networkIDTextView = (TextView) findViewById(R.id.network_id);
                        Spanned text = fromHtml("<b>" + getString(R.string.network_id) + "</b> " +
                                info.get("network_id"));
                        networkIDTextView.setText(text);
                        updatedTextView = (TextView) findViewById(R.id.updated);
                        text = fromHtml("<b>" + getString(R.string.updated) + "</b> " +
                                info.get("updated"));
                        updatedTextView.setText(text);

                        wifiRoleTextView = (TextView) findViewById(R.id.wifi_role);
                        text = fromHtml("<b>" + getString(R.string.role) + "</b> NONE");
                        wifiRoleTextView.setText(text);
                        wifiIpTextView = (TextView) findViewById(R.id.wifi_ip);
                        text = fromHtml("<b>" + getString(R.string.ip) + "</b> NONE");
                        wifiIpTextView.setText(text);
                        wifiConnectedToTextView = (TextView) findViewById(R.id.wifi_connect_to);
                        text = fromHtml("<b>" + getString(R.string.connected_to) + "</b> NONE");
                        wifiConnectedToTextView.setText(text);

                        bluetoothRoleTextView = (TextView) findViewById(R.id.bluetooth_role);
                        text = fromHtml("<b>" + getString(R.string.role) + "</b> NONE");
                        bluetoothRoleTextView.setText(text);
                        bluetoothIpTextView = (TextView) findViewById(R.id.bluetooth_ip);
                        text = fromHtml("<b>" + getString(R.string.ip) + "</b> NONE");
                        bluetoothIpTextView.setText(text);
                        bluetoothConnectedToTextView = (TextView) findViewById(R.id.bluetooth_connect_to);
                        text = fromHtml("<b>" + getString(R.string.connected_to) + "</b> NONE");
                        bluetoothConnectedToTextView.setText(text);
                        break;
                }
            }
        };

        updateUI(STATUS_READY_TO_START);

        activeGPS();

        // Hide soft keyboard at activity start-up
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
            case R.id.active_wifi:
                updateWifiStatus(isChecked);
                break;

            case R.id.active_bluetooth:
                updateBluetoothStatus(isChecked);
                break;

            default:
                break;
        }
    }

    /**
     * Spinner Callbacks
     */
    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
        // An item was selected. You can retrieve the selected item using
        // parent.getItemAtPosition(pos)
        //startup = false;
        switch (parent.getId()) {
            case R.id.aggressiveness_spinner:
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

                    default:
                        break;
                }
                break;

            case R.id.wifi_spinner:
                CheckBox wifiActivated = (CheckBox) findViewById(R.id.active_wifi);
                switch (pos) {
                    case 0:
                        if (roleWifi != LCCRole.CLIENT && wifiActivated.isChecked()) {
                            mService.stopWifiThread();
                            roleWifi = LCCRole.CLIENT;
                            updateWifiStatus(wifiActivated.isChecked());
                        } else {
                            roleWifi = LCCRole.CLIENT;
                        }
                        break;

                    case 1:
                        if (roleWifi != LCCRole.HOTSPOT && wifiActivated.isChecked()) {
                            mService.stopWifiThread();
                            roleWifi = LCCRole.HOTSPOT;
                            updateWifiStatus(wifiActivated.isChecked());
                        } else {
                            roleWifi = LCCRole.HOTSPOT;
                        }
                        break;

                    case 2:
                        if (roleWifi != LCCRole.CLIENT_HOTSPOT && wifiActivated.isChecked()) {
                            mService.stopWifiThread();
                            roleWifi = LCCRole.CLIENT_HOTSPOT;
                            updateWifiStatus(wifiActivated.isChecked());
                        } else {
                            roleWifi = LCCRole.CLIENT_HOTSPOT;
                        }
                        break;

                    default:
                        break;
                }
                break;

            case R.id.bluetooth_spinner:
                CheckBox bluetoothActivated = (CheckBox) findViewById(R.id.active_bluetooth);
                switch (pos) {
                    case 0:
                        if (roleBluetooth != LCCRole.CLIENT && bluetoothActivated.isChecked()) {
                            mService.stopBluetoothThread();
                            roleBluetooth = LCCRole.CLIENT;
                            updateBluetoothStatus(bluetoothActivated.isChecked());
                        } else {
                            roleBluetooth = LCCRole.CLIENT;
                        }
                        break;

                    case 1:
                        if (roleBluetooth != LCCRole.HOTSPOT && bluetoothActivated.isChecked()) {
                            mService.stopBluetoothThread();
                            roleBluetooth = LCCRole.HOTSPOT;
                            updateBluetoothStatus(bluetoothActivated.isChecked());
                        } else {
                            roleBluetooth = LCCRole.HOTSPOT;
                        }
                        break;

                    case 2:
                        if (roleBluetooth != LCCRole.CLIENT_HOTSPOT &&
                                bluetoothActivated.isChecked()) {
                            mService.stopBluetoothThread();
                            roleBluetooth = LCCRole.CLIENT_HOTSPOT;
                            updateBluetoothStatus(bluetoothActivated.isChecked());
                        } else {
                            roleBluetooth = LCCRole.CLIENT_HOTSPOT;
                        }
                        break;

                    default:
                        break;
                }
                break;
            default:
                break;
        }
    }

    @Override
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
                // Aggressiveness
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

                // Hotspot Wifi
                CheckBox wifiActivated = (CheckBox)findViewById(R.id.active_wifi);
                wifiActivated.setOnCheckedChangeListener(this);
                Spinner wifiSpinner = (Spinner) findViewById(R.id.wifi_spinner);
                wifiSpinner.setOnItemSelectedListener(this);
                wifiSpinner.setSelection(0);

                // Hotspot Bluetooth
                CheckBox bluetoothActivated = (CheckBox) findViewById(R.id.active_bluetooth);
                bluetoothActivated.setOnCheckedChangeListener(this);
                Spinner bluetoothSpinner = (Spinner) findViewById(R.id.bluetooth_spinner);
                bluetoothSpinner.setOnItemSelectedListener(this);
                bluetoothSpinner.setSelection(0);

                updatedTextView = (TextView) findViewById(R.id.updated);
                Spanned text = fromHtml("<b>" + getString(R.string.updated) + "</b> " +
                        getString(R.string.none));
                updatedTextView.setText(text);
                networkIDTextView = (TextView) findViewById(R.id.network_id);
                text = fromHtml("<b>" + getString(R.string.network_id) + "</b> " +
                        getString(R.string.none));
                networkIDTextView.setText(text);

                wifiRoleTextView = (TextView) findViewById(R.id.wifi_role);
                text = fromHtml("<b>" + getString(R.string.role) + "</b> " +
                        getString(R.string.none));
                wifiRoleTextView.setText(text);
                wifiIpTextView = (TextView) findViewById(R.id.wifi_ip);
                text = fromHtml("<b>" + getString(R.string.ip) + "</b> " +
                        getString(R.string.none));
                wifiIpTextView.setText(text);
                wifiConnectedToTextView = (TextView) findViewById(R.id.wifi_connect_to);
                text = fromHtml("<b>" + getString(R.string.connected_to) + "</b> " +
                        getString(R.string.none));
                wifiConnectedToTextView.setText(text);

                bluetoothRoleTextView = (TextView) findViewById(R.id.bluetooth_role);
                text = fromHtml("<b>" + getString(R.string.role) + "</b> " +
                        getString(R.string.none));
                bluetoothRoleTextView.setText(text);
                bluetoothIpTextView = (TextView) findViewById(R.id.bluetooth_ip);
                text = fromHtml("<b>" + getString(R.string.ip) + "</b> " +
                        getString(R.string.none));
                bluetoothIpTextView.setText(text);
                bluetoothConnectedToTextView = (TextView) findViewById(R.id.bluetooth_connect_to);
                text = fromHtml("<b>" + getString(R.string.connected_to) + "</b> " +
                        getString(R.string.none));
                bluetoothConnectedToTextView.setText(text);

                break;

            case STATUS_DEFAULT_VALUES:
                //startup = true;
                ((Spinner) findViewById(R.id.aggressiveness_spinner)).setSelection(1);

                ((Spinner) findViewById(R.id.wifi_spinner)).setSelection(0);
                ((CheckBox) findViewById(R.id.active_wifi)).setChecked(false);

                ((Spinner) findViewById(R.id.bluetooth_spinner)).setSelection(0);
                ((CheckBox) findViewById(R.id.active_bluetooth)).setChecked(false);
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

            default:
                break;
        }
    }


    private void restoreActivityState(){
    	System.out.println("MainActivity: restoreActivityState()");
		// Use shared preferences to restore the activity state
		SharedPreferences settings = getPreferences(MODE_PRIVATE);

        int aggressiveness = 1;
        int rs = Constants.DEFAULT_REGULAR_RS;
		int hc = Constants.DEFAULT_REGULAR_HC;
		int maxtbh = Constants.DEFAULT_REGULAR_MAXTBH;

        int wifi_role = 0;
        boolean wifi_thread = false;

        int bluetooth_role = 0;
        boolean bluetooth_thread = false;

        String networkIDText = getString(R.string.none);
        String updatedText = getString(R.string.none);

        String wifiRoleText = getString(R.string.none);
        String wifiIpText = getString(R.string.none);
        String wifiConnectedToText = getString(R.string.none);

        String bluetoothRoleText = getString(R.string.none);
        String bluetoothIpText = getString(R.string.none);
        String bluetoothConnectedToText = getString(R.string.none);

        aggressiveness = settings.getInt("aggressiveness", aggressiveness);
		rs = settings.getInt("rs", rs);
		hc = settings.getInt("hc", hc);
		maxtbh = settings.getInt("maxtbh", maxtbh);

        wifi_role = settings.getInt("wifi_role", wifi_role);
        wifi_thread = settings.getBoolean("wifi_thread", wifi_thread);

        bluetooth_role = settings.getInt("bluetooth_role", bluetooth_role);
        bluetooth_thread = settings.getBoolean("bluetooth_thread", bluetooth_thread);

        networkIDText = settings.getString("network_id_text", networkIDText);
        updatedText = settings.getString("updated_text", updatedText);

        wifiRoleText = settings.getString("wifi_role_text", wifiRoleText);
        wifiIpText = settings.getString("wifi_ip", wifiIpText);
        wifiConnectedToText = settings.getString("wifi_connected_to_text", wifiConnectedToText);

        bluetoothRoleText = settings.getString("bluetooth_role_text", bluetoothRoleText);
        bluetoothIpText = settings.getString("bluetooth_ip", bluetoothIpText);
        bluetoothConnectedToText = settings.getString("bluetooth_connected_to_text", bluetoothConnectedToText);

        ((Spinner) findViewById(R.id.aggressiveness_spinner)).setSelection(aggressiveness);
        ((EditText) findViewById(R.id.changeRolePeriodValue)).setText(String.valueOf(rs));
        ((EditText) findViewById(R.id.changeHotspotPeriodValue)).setText(String.valueOf(hc));
        ((EditText) findViewById(R.id.maxTimewaitToBeHotspotValue)).setText(String.valueOf(maxtbh));

        ((Spinner) findViewById(R.id.wifi_spinner)).setSelection(wifi_role);
        ((CheckBox) findViewById(R.id.active_wifi)).setChecked(wifi_thread);

        ((Spinner) findViewById(R.id.bluetooth_spinner)).setSelection(bluetooth_role);
        ((CheckBox) findViewById(R.id.active_bluetooth)).setChecked(bluetooth_thread);

        Spanned text = fromHtml("<b>" + getString(R.string.network_id) + "</b> " + networkIDText);
        networkIDTextView.setText(text);
        text = fromHtml("<b>" + getString(R.string.updated) + "</b> " + updatedText);
        updatedTextView.setText(text);

        text = fromHtml("<b>" + getString(R.string.role) + "</b> " + wifiRoleText);
        wifiRoleTextView.setText(text);
        text = fromHtml("<b>" + getString(R.string.ip) + "</b> " + wifiIpText);
        wifiIpTextView.setText(text);
        text = fromHtml("<b>" + getString(R.string.connected_to) + "</b> " + wifiConnectedToText);
        wifiConnectedToTextView.setText(text);

        text = fromHtml("<b>" + getString(R.string.role) + "</b> " + bluetoothRoleText);
        bluetoothRoleTextView.setText(text);
        text = fromHtml("<b>" + getString(R.string.ip) + "</b> " + bluetoothIpText);
        bluetoothIpTextView.setText(text);
        text = fromHtml("<b>" + getString(R.string.connected_to) + "</b> " +
                bluetoothConnectedToText);
        bluetoothConnectedToTextView.setText(text);
    }


    private void saveActivityState(){
		System.out.println("MainActivity: saveActivityState()");
		// Use shared preferences to save the activity state
		SharedPreferences settings = getPreferences(MODE_PRIVATE);
		SharedPreferences.Editor editor = settings.edit();
		editor.clear();
		
		// save parameters
        int aggressiveness = (int) ((Spinner) findViewById(R.id.aggressiveness_spinner)).getSelectedItemId();
        int rs = Integer.parseInt(((EditText) findViewById(R.id.changeRolePeriodValue)).getText().toString());
        int hc = Integer.parseInt(((EditText) findViewById(R.id.changeHotspotPeriodValue)).getText().toString());
        int maxtbh = Integer.parseInt(((EditText) findViewById(R.id.maxTimewaitToBeHotspotValue)).getText().toString());

        int wifi_role = (int) ((Spinner) findViewById(R.id.wifi_spinner)).getSelectedItemId();
        boolean wifi_thread = ((CheckBox) findViewById(R.id.active_wifi)).isChecked();

        int bluetooth_role = (int) ((Spinner) findViewById(R.id.bluetooth_spinner)).getSelectedItemId();
        boolean bluetooth_thread = ((CheckBox) findViewById(R.id.active_bluetooth)).isChecked();

        String text = networkIDTextView.getText().toString().split(":", 2)[1].replaceAll("\\s+", "");
        String networkIDText = text;
        text = updatedTextView.getText().toString().split(":", 2)[1].replaceAll("\\s+", "");
        String updatedText = text;

        text = wifiRoleTextView.getText().toString().split(":", 2)[1].replaceAll("\\s+", "");
        String wifiRoleText = text;
        text = wifiIpTextView.getText().toString().split(":", 2)[1].replaceAll("\\s+", "");
        String wifiIpText = text;
        text = wifiConnectedToTextView.getText().toString().split(":", 2)[1].replaceAll("\\s+", "");
        String wifiConnectedText = text;

        text = bluetoothRoleTextView.getText().toString().split(":", 2)[1].replaceAll("\\s+", "");
        String bluetoothRoleText = text;
        text = bluetoothIpTextView.getText().toString().split(":", 2)[1].replaceAll("\\s+", "");
        String blutoothIpText = text;
        text = bluetoothConnectedToTextView.getText().toString().split(":", 2)[1].replaceAll("\\s+", "");
        String bluetoothConnectedText = text;

        editor.putInt("aggressiveness", aggressiveness);
        editor.putInt("rs", rs);
		editor.putInt("hc", hc);
		editor.putInt("maxtbh", maxtbh);

        editor.putInt("wifi_role", wifi_role);
        editor.putBoolean("wifi_thread", wifi_thread);

        editor.putInt("bluetooth_role", bluetooth_role);
        editor.putBoolean("bluetooth_thread", bluetooth_thread);

        editor.putString("network_id_text", networkIDText);
        editor.putString("updated_text", updatedText);

        editor.putString("wifi_role_text", wifiRoleText);
        editor.putString("wifi_ip", wifiIpText);
        editor.putString("wifi_connected_to_text", wifiConnectedText);

        editor.putString("bluetooth_role_text", bluetoothRoleText);
        editor.putString("bluetooth_ip", blutoothIpText);
        editor.putString("bluetooth_connected_to_text", bluetoothConnectedText);


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
        System.out.println("MainActivity: onStart()");
        super.onStart();

        writeSettingsPermission(this);
        checkPermissions();

        if (!mBound) {
            // Bind to LocalService
            Intent intent = new Intent(this, LCCService.class);
            bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
        }
    }

    @Override
    protected void onPause() {
        System.out.println("MainActivity: onPause()");
        super.onPause();
        saveActivityState();
    }

    @Override
    protected void onRestart() {
        System.out.println("MainActivity: onRestart()");
        super.onRestart();
    }

    @Override
    protected void onResume() {
        System.out.println("MainActivity: onResume()");
        super.onResume();
        restoreActivityState();
    }

    @Override
    protected void onStop() {
        System.out.println("MainActivity: onStop()");
        super.onStop();

        // Unbind from the service
        if (!mService.wifiThreadIsActive() && !mService.bluetoothThreadIsActive()) {
            unbindService(mConnection);
            mBound = false;
        }
    }

    private void updateWifiStatus(boolean isChecked) {
        if (mBound) {
            if (!mService.wifiThreadIsActive()) {
                int rs = Integer.parseInt(((EditText) findViewById(R.id.changeRolePeriodValue)).getText().toString());
                int hc = Integer.parseInt(((EditText) findViewById(R.id.changeHotspotPeriodValue)).getText().toString());
                int maxtbh = Integer.parseInt(((EditText) findViewById(R.id.maxTimewaitToBeHotspotValue)).getText().toString());

                mService.startWifiThread(context, roleWifi, rs, hc, maxtbh, uiHandler);
            }
            if (!isChecked) {
                mService.stopWifiThread();
            }
        }
    }

    private void updateBluetoothStatus(boolean isChecked) {
        if (mBound) {
            if (!mService.bluetoothThreadIsActive()) {
                int rs = Integer.parseInt(((EditText) findViewById(R.id.changeRolePeriodValue)).getText().toString());
                int hc = Integer.parseInt(((EditText) findViewById(R.id.changeHotspotPeriodValue)).getText().toString());
                int maxtbh = Integer.parseInt(((EditText) findViewById(R.id.maxTimewaitToBeHotspotValue)).getText().toString());

                mService.startBluetoothThread(context, roleBluetooth, rs, hc, maxtbh, uiHandler);
            }
            if (!isChecked) {
                mService.stopBluetoothThread();
            }
        }
    }

    private void activeGPS() {
        try {
            int locationMode = 0;
            String locationProviders;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                locationMode = Settings.Secure.getInt(context.getContentResolver(),
                        Settings.Secure.LOCATION_MODE);
                if (locationMode == Settings.Secure.LOCATION_MODE_OFF) {
                    showMessageOKCancel("You have to enable GPS",
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                                }
                            });
                }

            } else {
                locationProviders = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.LOCATION_PROVIDERS_ALLOWED);
                if (TextUtils.isEmpty(locationProviders)) {
                    final Intent locationIntent = new Intent();
                    locationIntent.setClassName("com.android.settings",
                            "com.android.settings.widget.SettingsAppWidgetProvider");
                    locationIntent.addCategory(Intent.CATEGORY_ALTERNATIVE);
                    locationIntent.setData(Uri.parse("3"));
                    sendBroadcast(locationIntent);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings("deprecation")
    public static Spanned fromHtml(String html) {
        Spanned result;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            result = Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY);
        } else {
            result = Html.fromHtml(html);
        }
        return result;
    }

    /**
     * Defines callbacks for service binding, passed to bindService()
     */
    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            // We've bound to LCCService, cast the IBinder and get LCCService instance
            System.out.println("MainActivity: onServiceConnected()");
            LCCService.LCCBinder binder = (LCCService.LCCBinder) service;
            mService = binder.getService();
            mBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName className) {
            // This is called when the connection with the service has been
            // unexpectedly disconnected -- that is, its process crashed.
            System.out.println("MainActivity: onServiceDisconnected()");
            updateUI(STATUS_DEFAULT_VALUES);
            mService = null;
            mBound = false;
        }
    };

    private static void writeSettingsPermission(Activity context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.System.canWrite(context)) {
                Intent intent = new Intent(android.provider.Settings.ACTION_MANAGE_WRITE_SETTINGS);
                intent.setData(Uri.parse("package:" + context.getPackageName()));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
            }
        }
    }

    private void checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            List<String> permissionsNeeded = new ArrayList<String>();

            final List<String> permissionsList = new ArrayList<>();
            if (!addPermission(permissionsList, Manifest.permission.ACCESS_COARSE_LOCATION))
                permissionsNeeded.add("GPS");
            if (!addPermission(permissionsList, Manifest.permission.READ_EXTERNAL_STORAGE))
                permissionsNeeded.add("Read external storage");
            if (!addPermission(permissionsList, Manifest.permission.WRITE_EXTERNAL_STORAGE))
                permissionsNeeded.add("Write external storage");

            if (permissionsList.size() > 0) {
                if (permissionsNeeded.size() > 0) {
                    // Need Rationale
                    String message = "You need to grant access to " + permissionsNeeded.get(0);
                    for (int i = 1; i < permissionsNeeded.size(); i++)
                        message = message + ", " + permissionsNeeded.get(i);
                    showMessageOKCancel(message,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    requestPermissions(permissionsList.toArray(
                                            new String[permissionsList.size()]),
                                            REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS);
                                }
                            });
                }
                requestPermissions(permissionsList.toArray(new String[permissionsList.size()]),
                        REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS);
            }
        }
    }

    private boolean addPermission(List<String> permissionsList, String permission) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
                permissionsList.add(permission);
                // Check for Rationale Option
                if (!shouldShowRequestPermissionRationale(permission))
                    return false;
            }
            return true;
        } else {
            return false;
        }
    }

    private void showMessageOKCancel(String message, DialogInterface.OnClickListener okListener) {
        new AlertDialog.Builder(MainActivity.this)
                .setMessage(message)
                .setPositiveButton("OK", okListener)
                .setNegativeButton("Cancel", null)
                .create()
                .show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS: {
                Map<String, Integer> perms = new HashMap<String, Integer>();
                // Initial
                perms.put(Manifest.permission.ACCESS_COARSE_LOCATION,
                        PackageManager.PERMISSION_GRANTED);
                perms.put(Manifest.permission.READ_EXTERNAL_STORAGE,
                        PackageManager.PERMISSION_GRANTED);
                perms.put(Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        PackageManager.PERMISSION_GRANTED);
                // Fill with results
                for (int i = 0; i < permissions.length; i++)
                    perms.put(permissions[i], grantResults[i]);
                // Check for ACCESS_COARSE_LOCATION
                if (perms.get(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                        && perms.get(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
                        && perms.get(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                    // All Permissions Granted
                    Toast.makeText(MainActivity.this, "All permissions are allowed", Toast.LENGTH_SHORT)
                            .show();
                } else {
                    // Permission Denied
                    Toast.makeText(MainActivity.this, "Some permissions are denied", Toast.LENGTH_SHORT)
                            .show();
                }
            }
            break;

            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }


    private class UIHandler extends Handler {

        private static final int UPDATE_UI_WIFI = 1;
        private static final int UPDATE_UI_BLUETOOTH = 2;
        private Context context;

        public UIHandler(Looper looper, Context context){
            super(looper);
            this.context = context;
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case UPDATE_UI_WIFI:
                    try {
                        HashMap<String, String> info = (HashMap<String, String>) msg.obj;

                        networkIDTextView = (TextView) findViewById(R.id.network_id);
                        Spanned text = fromHtml("<b>" + getString(R.string.network_id) + "</b> " +
                                info.get("network_id"));
                        networkIDTextView.setText(text);
                        updatedTextView = (TextView) findViewById(R.id.updated);
                        text = fromHtml("<b>" + getString(R.string.updated) + "</b> " +
                                info.get("updated"));
                        updatedTextView.setText(text);

                        wifiRoleTextView = (TextView) findViewById(R.id.wifi_role);
                        text = fromHtml("<b>" + getString(R.string.role) + "</b> " +
                                info.get("wifi_role"));
                        wifiRoleTextView.setText(text);
                        wifiIpTextView = (TextView) findViewById(R.id.wifi_ip);
                        text = fromHtml("<b>" + getString(R.string.ip) + "</b> " +
                                info.get("wifi_ip"));
                        wifiIpTextView.setText(text);
                        wifiConnectedToTextView = (TextView) findViewById(R.id.wifi_connect_to);
                        text = fromHtml("<b>" + getString(R.string.connected_to) + "</b> " +
                                info.get("wifi_connected_to"));
                        wifiConnectedToTextView.setText(text);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    break;

                case UPDATE_UI_BLUETOOTH:
                    try {
                        HashMap<String, String> info = (HashMap<String, String>) msg.obj;

                        networkIDTextView = (TextView) findViewById(R.id.network_id);
                        Spanned text = fromHtml("<b>" + getString(R.string.network_id) + "</b> " +
                                info.get("network_id"));
                        networkIDTextView.setText(text);
                        updatedTextView = (TextView) findViewById(R.id.updated);
                        text = fromHtml("<b>" + getString(R.string.updated) + "</b> " +
                                info.get("updated"));
                        updatedTextView.setText(text);

                        bluetoothRoleTextView = (TextView) findViewById(R.id.bluetooth_role);
                        text = fromHtml("<b>" + getString(R.string.role) + "</b> " +
                                info.get("bluetooth_role"));
                        bluetoothRoleTextView.setText(text);
                        bluetoothIpTextView = (TextView) findViewById(R.id.bluetooth_ip);
                        text = fromHtml("<b>" + getString(R.string.ip) + "</b> " +
                                info.get("bluetooth_ip"));
                        bluetoothIpTextView.setText(text);
                        bluetoothConnectedToTextView = (TextView) findViewById(R.id.bluetooth_connect_to);
                        text = fromHtml("<b>" + getString(R.string.connected_to) + "</b> " +
                                info.get("bluetooth_connected_to"));
                        bluetoothConnectedToTextView.setText(text);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    break;

                default:
                    HashMap<String, String> info = (HashMap<String, String>) msg.obj;

                    networkIDTextView = (TextView) findViewById(R.id.network_id);
                    Spanned text = fromHtml("<b>" + getString(R.string.network_id) + "</b> " +
                            info.get("network_id"));
                    networkIDTextView.setText(text);
                    updatedTextView = (TextView) findViewById(R.id.updated);
                    text = fromHtml("<b>" + getString(R.string.updated) + "</b> " +
                            info.get("updated"));
                    updatedTextView.setText(text);

                    wifiRoleTextView = (TextView) findViewById(R.id.wifi_role);
                    text = fromHtml("<b>" + getString(R.string.role) + "</b> NONE");
                    wifiRoleTextView.setText(text);
                    wifiIpTextView = (TextView) findViewById(R.id.wifi_ip);
                    text = fromHtml("<b>" + getString(R.string.ip) + "</b> NONE");
                    wifiIpTextView.setText(text);
                    wifiConnectedToTextView = (TextView) findViewById(R.id.wifi_connect_to);
                    text = fromHtml("<b>" + getString(R.string.connected_to) + "</b> NONE");
                    wifiConnectedToTextView.setText(text);

                    bluetoothRoleTextView = (TextView) findViewById(R.id.bluetooth_role);
                    text = fromHtml("<b>" + getString(R.string.role) + "</b> NONE");
                    bluetoothRoleTextView.setText(text);
                    bluetoothIpTextView = (TextView) findViewById(R.id.bluetooth_ip);
                    text = fromHtml("<b>" + getString(R.string.ip) + "</b> NONE");
                    bluetoothIpTextView.setText(text);
                    bluetoothConnectedToTextView = (TextView) findViewById(R.id.bluetooth_connect_to);
                    text = fromHtml("<b>" + getString(R.string.connected_to) + "</b> NONE");
                    bluetoothConnectedToTextView.setText(text);
                    break;
            }
        }
    }

    private class UIHandlerThread extends HandlerThread {

        Handler handler;
        private static final int UPDATE_UI_WIFI = 1;
        private static final int UPDATE_UI_BLUETOOTH = 2;

        public UIHandlerThread(String name) {
            super(name);
        }

        @Override
        protected void onLooperPrepared() {
            handler = new Handler(getLooper()) {
                @Override
                public void handleMessage(Message msg) {
                    // process incoming messages here
                    // this will run in non-ui/background thread
                    switch (msg.what) {
                        case UPDATE_UI_WIFI:
                            try {
                                HashMap<String, String> info = (HashMap<String, String>) msg.obj;

                                networkIDTextView = (TextView) findViewById(R.id.network_id);
                                Spanned text = fromHtml("<b>" + getString(R.string.network_id) +
                                        "</b> " + info.get("network_id"));
                                networkIDTextView.setText(text);
                                updatedTextView = (TextView) findViewById(R.id.updated);
                                text = fromHtml("<b>" + getString(R.string.updated) + "</b> " +
                                        info.get("updated"));
                                updatedTextView.setText(text);

                                wifiRoleTextView = (TextView) findViewById(R.id.wifi_role);
                                text = fromHtml("<b>" + getString(R.string.role) + "</b> " +
                                        info.get("wifi_role"));
                                wifiRoleTextView.setText(text);
                                wifiIpTextView = (TextView) findViewById(R.id.wifi_ip);
                                text = fromHtml("<b>" + getString(R.string.ip) + "</b> " +
                                        info.get("wifi_ip"));
                                wifiIpTextView.setText(text);
                                wifiConnectedToTextView = (TextView) findViewById(R.id.wifi_connect_to);
                                text = fromHtml("<b>" + getString(R.string.connected_to) +
                                        "</b> " + info.get("wifi_connected_to"));
                                wifiConnectedToTextView.setText(text);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            break;

                        case UPDATE_UI_BLUETOOTH:
                            try {
                                HashMap<String, String> info = (HashMap<String, String>) msg.obj;

                                networkIDTextView = (TextView) findViewById(R.id.network_id);
                                Spanned text = fromHtml("<b>" + getString(R.string.network_id) +
                                        "</b> " + info.get("network_id"));
                                networkIDTextView.setText(text);
                                updatedTextView = (TextView) findViewById(R.id.updated);
                                text = fromHtml("<b>" + getString(R.string.updated) + "</b> " +
                                        info.get("updated"));
                                updatedTextView.setText(text);

                                bluetoothRoleTextView = (TextView) findViewById(R.id.bluetooth_role);
                                text = fromHtml("<b>" + getString(R.string.role) + "</b> " +
                                        info.get("bluetooth_role"));
                                bluetoothRoleTextView.setText(text);
                                bluetoothIpTextView = (TextView) findViewById(R.id.bluetooth_ip);
                                text = fromHtml("<b>" + getString(R.string.ip) + "</b> " +
                                        info.get("bluetooth_ip"));
                                bluetoothIpTextView.setText(text);
                                bluetoothConnectedToTextView = (TextView) findViewById(R.id.bluetooth_connect_to);
                                text = fromHtml("<b>" + getString(R.string.connected_to) + "</b> " +
                                        info.get("bluetooth_connected_to"));
                                bluetoothConnectedToTextView.setText(text);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            break;

                        default:
                            HashMap<String, String> info = (HashMap<String, String>) msg.obj;

                            networkIDTextView = (TextView) findViewById(R.id.network_id);
                            Spanned text = fromHtml("<b>" + getString(R.string.network_id) +
                                    "</b> " + info.get("network_id"));
                            networkIDTextView.setText(text);
                            updatedTextView = (TextView) findViewById(R.id.updated);
                            text = fromHtml("<b>" + getString(R.string.updated) + "</b> " +
                                    info.get("updated"));
                            updatedTextView.setText(text);

                            wifiRoleTextView = (TextView) findViewById(R.id.wifi_role);
                            text = fromHtml("<b>" + getString(R.string.role) + "</b> NONE");
                            wifiRoleTextView.setText(text);
                            wifiIpTextView = (TextView) findViewById(R.id.wifi_ip);
                            text = fromHtml("<b>" + getString(R.string.ip) + "</b> NONE");
                            wifiIpTextView.setText(text);
                            wifiConnectedToTextView = (TextView) findViewById(R.id.wifi_connect_to);
                            text = fromHtml("<b>" + getString(R.string.connected_to) + "</b> NONE");
                            wifiConnectedToTextView.setText(text);

                            bluetoothRoleTextView = (TextView) findViewById(R.id.bluetooth_role);
                            text = fromHtml("<b>" + getString(R.string.role) + "</b> NONE");
                            bluetoothRoleTextView.setText(text);
                            bluetoothIpTextView = (TextView) findViewById(R.id.bluetooth_ip);
                            text = fromHtml("<b>" + getString(R.string.ip) + "</b> NONE");
                            bluetoothIpTextView.setText(text);
                            bluetoothConnectedToTextView = (TextView) findViewById(R.id.bluetooth_connect_to);
                            text = fromHtml("<b>" + getString(R.string.connected_to) + "</b> NONE");
                            bluetoothConnectedToTextView.setText(text);
                            break;
                    }
                }
            };
        }
    }

}
