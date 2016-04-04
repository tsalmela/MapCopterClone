package fi.oulu.mapcopter;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.TextureView;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import dji.sdk.Camera.DJICamera;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class MapActivity extends AppCompatActivity implements MapCopterRealManager.CopterStatusChangeListener, OnMapReadyCallback {
    private static final String TAG = MapActivity.class.getSimpleName();

    private static final int UI_ANIMATION_DELAY = 500;
    private static final String FLAG_CONNECTION_CHANGE = "fi_oulu_mapcopter_connection_change";
    private final Handler mHideHandler = new Handler();
    private final Runnable mHideRunnable = new Runnable() {
        @Override
        public void run() {
            hide();
        }
    };
    //private View mContentView;
    private Handler mHandler;
    private Runnable updateRunnable = new Runnable() {
        @Override
        public void run() {
            Intent intent = new Intent(FLAG_CONNECTION_CHANGE);
            Log.d(TAG, "update runnable");
            sendBroadcast(intent);
        }
    };
    private TextureView mCameraView;
    private DJICamera mCamera;
    private MapCopterManager mapCopterManager;
    private VideoSurfaceListener surfaceListener;
    protected BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            onProductChange();
        }
    };
    private GoogleMap mMap;
    private View mContentView;
    private Button mStopButton;
    private SeekBar seekBar;
    private TextView textView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);
        initializeVariables();

        // Initialize the textview with '0'.
        textView.setText("Korkeus: " + seekBar.getProgress() + "/" + seekBar.getMax());
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            int progress = 0;

            @Override
            public void onProgressChanged(SeekBar seekBar, int progresValue, boolean fromUser) {
                progress = progresValue;
                Toast.makeText(getApplicationContext(), "Muutetaan korkeutta", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                Toast.makeText(getApplicationContext(), "Started tracking seekbar", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                String progressText = ("" + progress).format("%1$-" + 3 + "s", "XXX").replaceAll(" ", "0");
                textView.setText("Korkeus: " + progressText + "/" + seekBar.getMax());
                Toast.makeText(getApplicationContext(), "Stopped tracking seekbar", Toast.LENGTH_SHORT).show();
            }
        });

        seekBar.setRotation(270);

        mHandler = new Handler(Looper.getMainLooper());
        mCameraView = (TextureView) findViewById(R.id.camera_view);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        mStopButton = (Button) findViewById(R.id.button_stop);

        mStopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "STOP button pushed: ");
            }
        });

        mapCopterManager = MapCopterManager.createManager(this, this);
        mapCopterManager.initManager();

        IntentFilter filter = new IntentFilter();
        filter.addAction(FLAG_CONNECTION_CHANGE);

        registerReceiver(mReceiver, filter);

    }

    public static String LPad(String str, Integer length, char car) {
        return str
                +
                String.format("%" + (length - str.length()) + "s", "")
                        .replace(" ", String.valueOf(car));
    }

    // A private method to help us initialize our variables.
    private void initializeVariables() {
        seekBar = (SeekBar) findViewById(R.id.seekBar);
        textView = (TextView) findViewById(R.id.textView);
    }

    private void onProductChange() {
        if (surfaceListener != null) {
            surfaceListener.initPreviewer();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mReceiver);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (surfaceListener == null) {
            surfaceListener = new VideoSurfaceListener(this, mapCopterManager);
        }
        mCameraView.setSurfaceTextureListener(surfaceListener);
        surfaceListener.initPreviewer();
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        // Trigger the initial hide() shortly after the activity has been
        // created, to briefly hint to the user that UI controls
        // are available.
        delayedHide(UI_ANIMATION_DELAY);
    }

    @SuppressLint("InlinedApi")
    private void hide() {
        /*
        mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
                */
    }

    @SuppressLint("InlinedApi")
    private void show() {
        // Show the system bar
        /*
        mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
                */
    }

    /**
     * Schedules a call to hide() in [delay] milliseconds, canceling any
     * previously scheduled calls.
     */
    private void delayedHide(int delayMillis) {
        mHideHandler.removeCallbacks(mHideRunnable);
        mHideHandler.postDelayed(mHideRunnable, delayMillis);
    }

    @Override
    public void onStatusChanged() {
        mHandler.postDelayed(updateRunnable, 500);
        mHandler.removeCallbacks(updateRunnable);

    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Add a marker and move the camera
        LatLng lipasto = new LatLng(65.0591, 25.466549);
        mMap.addMarker(new MarkerOptions().position(lipasto).title("Lipasto"));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(lipasto));
    }
}
