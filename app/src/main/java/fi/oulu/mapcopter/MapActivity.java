package fi.oulu.mapcopter;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;

import java.util.ArrayList;
import java.util.List;

import butterknife.ButterKnife;
import butterknife.OnClick;
import fi.oulu.mapcopter.copter.AircraftPositionChangeListener;
import fi.oulu.mapcopter.copter.CameraManager;
import fi.oulu.mapcopter.copter.CopterManager;
import fi.oulu.mapcopter.event.CopterConnectionEvent;
import fi.oulu.mapcopter.event.CopterStatusChangeEvent;
import fi.oulu.mapcopter.view.TouchableMapFragment;
import fi.oulu.mapcopter.view.VideoSurfaceListener;

public class MapActivity extends AppCompatActivity implements AircraftPositionChangeListener {
    private static final String TAG = MapActivity.class.getSimpleName();
    public static final double MAP_BOUNDS_LIMIT_LATITUDE = 0.0035;
    public static final double MAP_BOUNDS_LIMIT_LONGITUDE = 0.01;

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
    private CopterManager mapCopterManager;
    private GoogleMap mMap;
    private Marker aircraftLocationMarker;
    private Marker destinationMarker;

    private Bus eventBus;
    private Polygon boundsPolygon;
    private View mContentView;
    private Button mStopButton;
    private SeekBar seekBar;
    private TextView textView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);
        ButterKnife.bind(this);
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

        prepareMap();

        eventBus = MapCopterApplication.getDefaultBus();
        mapCopterManager = MapCopterApplication.getCopterManager();
        mapCopterManager.setCopterPositionChangeListener(this);

        TextureView mCameraView = ButterKnife.findById(this, R.id.camera_view);
        VideoSurfaceListener surfaceListener = new VideoSurfaceListener(this, mapCopterManager.getCameraManager());
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
    }

    @OnClick(R.id.button_stop)
    public void onStopButtonClicked() {
        mapCopterManager.stopCopter();
        //LatLng target = mMap.getCameraPosition().target;
        //onAircraftPositionChanged(target.latitude, target.longitude, 0, 0);
    }

    private void moveToMapCenter() {
        if (mMap != null) {
            Log.d(TAG, "Current aircraft position: " + mapCopterManager.getCurrentPosition().toString());
            Log.d(TAG, "Moving to: " + mMap.getCameraPosition().target.toString());

            LatLng target = mMap.getCameraPosition().target;
            destinationMarker.setPosition(target);
            mapCopterManager.moveToPos(target.latitude, target.longitude);
            displayToast("Moving to " + target.toString());
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        eventBus.register(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        eventBus.unregister(this);
    }

    private void onMapTouchEnd() {
        LatLng centerOfMap = mMap.getCameraPosition().target;
        Log.d(TAG, centerOfMap.toString());
        moveToMapCenter();
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

    private void prepareMap() {
        TouchableMapFragment mapFragment = (TouchableMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.setTouchListener(new TouchableMapFragment.TouchListener() {
            @Override
            public void onTouchStart() {
            }

            @Override
            public void onTouchEnd() {
                onMapTouchEnd();
            }
        });
        mapFragment.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(GoogleMap googleMap) {
                mMap = googleMap;
                final LatLng lipasto = new LatLng(65.0591, 25.466549);

                destinationMarker = mMap.addMarker(new MarkerOptions()
                        .position(new LatLng(0, 0))
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)));
                aircraftLocationMarker = mMap.addMarker(new MarkerOptions()
                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.icon_aircraft))
                        .anchor(0.5f, 0.5f)
                        .position(new LatLng(0, 0)));

                drawBoundsRectangle(lipasto.latitude, lipasto.longitude);

                //mMap.moveCamera(CameraUpdateFactory.newLatLng(lipasto));
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(lipasto, 15));

                mMap.setOnCameraChangeListener(new GoogleMap.OnCameraChangeListener() {
                    @Override
                    public void onCameraChange(CameraPosition cameraPosition) {
                        LatLng currentPosition = mapCopterManager.getCurrentPosition();
                        //LatLng currentPosition = lipasto;
                        double latDifference = cameraPosition.target.latitude - currentPosition.latitude;
                        double longDifference = cameraPosition.target.longitude - currentPosition.longitude;

                        //Log.d(TAG, "latitude difference " + latDifference);
                        //Log.d(TAG, "longitude difference " + longDifference);

                        double newLat = cameraPosition.target.latitude;

                        boolean moveCamera = false;

                        if (latDifference > MAP_BOUNDS_LIMIT_LATITUDE) {
                            moveCamera = true;
                            //Log.d(TAG, "moving camera lat +");
                            newLat = currentPosition.latitude + MAP_BOUNDS_LIMIT_LATITUDE;
                        } else if (latDifference < -MAP_BOUNDS_LIMIT_LATITUDE) {
                            moveCamera = true;
                            //Log.d(TAG, "moving camera lat -");
                            newLat = currentPosition.latitude - MAP_BOUNDS_LIMIT_LATITUDE;
                        }


                        double newLong = cameraPosition.target.longitude;

                        if (longDifference > MAP_BOUNDS_LIMIT_LONGITUDE) {
                            moveCamera = true;
                            //Log.d(TAG, "moving camera long +");
                            newLong = currentPosition.longitude + MAP_BOUNDS_LIMIT_LONGITUDE;
                        } else if (longDifference < -MAP_BOUNDS_LIMIT_LONGITUDE) {
                            moveCamera = true;
                            //Log.d(TAG, "moving camera long -");
                            newLong = currentPosition.longitude - MAP_BOUNDS_LIMIT_LONGITUDE;
                        }

                        if (moveCamera) {
                            mMap.moveCamera(CameraUpdateFactory.newLatLng(new LatLng(newLat, newLong)));
                        }
                    }
                });
            }
        });
    }

    @Subscribe
    public void onCopterConnectionChanged(CopterConnectionEvent event) {
        if (event.isConnected()) {
            displayToast("Connected to aircraft");
        } else {
            displayToast("Disconnected from aircraft");
        }
    }

    @Subscribe
    public void onCopterStatusChanged(CopterStatusChangeEvent event) {
        displayToast(event.getMessage());
    }

    private void displayToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    private void drawBoundsRectangle(double latitude, double longitude) {

        List<LatLng> points = new ArrayList<>();
        points.add(new LatLng(latitude + MAP_BOUNDS_LIMIT_LATITUDE, longitude + MAP_BOUNDS_LIMIT_LONGITUDE));
        points.add(new LatLng(latitude + MAP_BOUNDS_LIMIT_LATITUDE, longitude - MAP_BOUNDS_LIMIT_LONGITUDE));
        points.add(new LatLng(latitude - MAP_BOUNDS_LIMIT_LATITUDE, longitude - MAP_BOUNDS_LIMIT_LONGITUDE));
        points.add(new LatLng(latitude - MAP_BOUNDS_LIMIT_LATITUDE, longitude + MAP_BOUNDS_LIMIT_LONGITUDE));

        if (boundsPolygon == null) {
            boundsPolygon = mMap.addPolygon(new PolygonOptions().addAll(points));
        } else {
            boundsPolygon.setPoints(points);
        }
    }

    @Override
    public void onAircraftPositionChanged(double latitude, double longitude, float altitude, double rotation) {
        if (aircraftLocationMarker != null) {
            aircraftLocationMarker.setPosition(new LatLng(latitude, longitude));
            aircraftLocationMarker.setRotation((float) rotation);

            if (mMap != null) {
                double lat = aircraftLocationMarker.getPosition().latitude;
                double lon = aircraftLocationMarker.getPosition().longitude;

                drawBoundsRectangle(lat, lon);


            }
        }
    }
}
