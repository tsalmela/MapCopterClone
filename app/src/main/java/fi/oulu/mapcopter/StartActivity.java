package fi.oulu.mapcopter;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.usb.UsbAccessory;
import android.hardware.usb.UsbManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import fi.oulu.mapcopter.event.CopterConnectionEvent;

/**
 * Starting activity, used for preparing the copter (connection, gps, compass)
 * Transitions to {@link MapActivity} when takeoff button is pressed {@link #onStartButton()}.
 *
 */
public class StartActivity extends AppCompatActivity {
    private static final String TAG = StartActivity.class.getSimpleName();
    private static final String ACTION_USB_PERMISSION = "fi.oulu.mapcopter.USB_PERMISSION";

    private Bus eventBus;

    @Bind(R.id.progress)
    ProgressBar gpsStatusBar;

    private boolean gpsStatusCheckRunning;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);
        ButterKnife.bind(this);
        eventBus = MapCopterApplication.getDefaultBus();

        requestUsbPermission();
        requestAppPermissions();

        startGpsStatusChecker();
    }

    public int getGpsSignalStatus() {
        return MapCopterApplication.getCopterManager().getGPSStatus();
    }

    /**
     * Starts GPS status checking loop on another thread.
     * It checks the GPS status and updates the UI accordingly.
     */
    private void startGpsStatusChecker() {
        gpsStatusCheckRunning = true;
        new Thread(new Runnable() {
            public void run() {
                while (gpsStatusCheckRunning) {
                    int status = getGpsSignalStatus();
                    if (status == 255) {
                        status = 0;
                    } else {
                        status = status + 1;
                    }

                    final int progress = status * 20;

                    runOnUiThread(new Runnable() {
                        public void run() {
                            if (gpsStatusBar != null) {
                                gpsStatusBar.setProgress(progress);
                            }
                        }
                    });

                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        gpsStatusCheckRunning = false;
                    }
                }
            }
        }).start();
    }

    /**
     * Request permission to use any connected USB devices.
     * Required on some devices if starting the application when the copter is
     * already connected to the device via USB.
     */
    private void requestUsbPermission() {
        UsbManager manager = (UsbManager) getSystemService(Context.USB_SERVICE);
        UsbAccessory[] accessoryList = manager.getAccessoryList();
        if (accessoryList != null) {
            PendingIntent intent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
//            IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
            for (UsbAccessory accessory : accessoryList) {
                Log.d(TAG, "Requesting permission for " + accessory.toString() + " " + accessory.getDescription());
                manager.requestPermission(accessory, intent);
            }
        }
    }

    /**
     * Request the necessary permission to use the DJI sdk.
     * Preparation for Android Marshmallow (api 23) which requires applications
     * to request permissions at runtime instead of when installing the app.
     *
     * Currently not necessary since the targetSdkVersion is 22
     * because the current DJI SDK version (3.1) does not support android 23 properly.
     * see app/build.gradle > targetSdkVersion
     */
    private void requestAppPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            int permission = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
            if (permission != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.READ_PHONE_STATE,
                        Manifest.permission.INTERNET,
                        Manifest.permission.WAKE_LOCK,
                        Manifest.permission.ACCESS_WIFI_STATE,
                        Manifest.permission.CHANGE_WIFI_STATE,
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.ACCESS_NETWORK_STATE,
                }, 123);
            }
        }
    }

    @OnClick(R.id.buttonStart)
    public void onStartButton() {
        gpsStatusCheckRunning = false;
        Intent myIntent = new Intent(StartActivity.this, MapActivity.class);
        StartActivity.this.startActivity(myIntent);
        MapCopterApplication.getCopterManager().takeOff();
    }

    @Override
    protected void onStart() {
        super.onStart();
        eventBus.register(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        gpsStatusCheckRunning = false;
        eventBus.unregister(this);
    }

    @Subscribe
    public void onCopterConnectionChanged(CopterConnectionEvent event) {
        if (event.isConnected()) {
            TextView mInfo = (TextView) findViewById(R.id.textView_Connection);
            if (event.getModel() != null && !event.getModel().isEmpty()) {
                mInfo.setText(event.getModel());
            } else {
                mInfo.setText(R.string.activity_start_text_connected);
            }
        } else {
            TextView mInfo = (TextView) findViewById(R.id.textView_Connection);
            mInfo.setText(R.string.activity_start_text_not_connected);
        }
    }

    @OnClick(R.id.button_calibrate)
    public void onCalibrateClicked() {
        Intent intent = new Intent(this, CalibrationDialogActivity.class);
        startActivity(intent);
    }

    @OnClick(R.id.button_remoteAccessInfo)
    public void onRemoteInfoClicked() {
        Intent intent = new Intent(this, RemoteInfoDialogActivity.class);
        startActivity(intent);
    }
}
