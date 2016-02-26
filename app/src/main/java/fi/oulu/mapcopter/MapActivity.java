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

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import dji.sdk.Camera.DJICamera;
import dji.sdk.FlightController.DJIFlightController;
import dji.sdk.FlightController.DJIFlightControllerDataType;
import dji.sdk.FlightController.DJIFlightControllerDelegate;
import dji.sdk.MissionManager.DJIMission;
import dji.sdk.MissionManager.DJIMissionManager;
import dji.sdk.MissionManager.DJIWaypoint;
import dji.sdk.MissionManager.DJIWaypointMission;
import dji.sdk.Products.DJIAircraft;
import dji.sdk.SDKManager.DJISDKManager;
import dji.sdk.base.DJIBaseComponent;
import dji.sdk.base.DJIBaseProduct;
import dji.sdk.base.DJIError;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class MapActivity extends AppCompatActivity implements MapCopterRealManager.CopterStatusChangeListener, OnMapReadyCallback {
    private static final String TAG = MapActivity.class.getSimpleName();

    private static final int UI_ANIMATION_DELAY = 500;
    private static final String FLAG_CONNECTION_CHANGE = "fi_oulu_mapcopter_connection_change";

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
    private GoogleMap mMap;
    private View mContentView;
    private Button mStopButton;
    private TouchableMapFragment mapFragment;
    private Marker aircraftLocationMarker;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        mHandler = new Handler(Looper.getMainLooper());
        mCameraView = (TextureView) findViewById(R.id.camera_view);

        mapFragment = (TouchableMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);


        mStopButton = (Button) findViewById(R.id.button_stop);
        mStopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, mMap.getCameraPosition().target.toString());

                DJIWaypointMission djiMission = new DJIWaypointMission();
                djiMission.maxFlightSpeed = 14;
                djiMission.autoFlightSpeed = 4;
                LatLng target = mMap.getCameraPosition().target;

                //djiMission.addWaypoint(new DJIWaypoint(target.latitude, target.longitude, 30));


                DJIBaseProduct product = mapCopterManager.getProduct();
                if (product instanceof DJIAircraft) {
                    Log.d(TAG, "product is dji aircraft");
                    DJIAircraft aircraft = (DJIAircraft) product;


                    DJIFlightController flightController = aircraft.getFlightController();

                    if (flightController != null) {
                        flightController.setUpdateSystemStateCallback(new DJIFlightControllerDelegate.FlightControllerUpdateSystemStateCallback() {
                            @Override
                            public void onResult(final DJIFlightControllerDataType.DJIFlightControllerCurrentState state) {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        DJIFlightControllerDataType.DJILocationCoordinate3D currentLocation = state.getAircraftLocation();
                                        LatLng position = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());
                                        aircraftLocationMarker.setPosition(position);
                                    }
                                });
                            }
                        });


                        DJIFlightControllerDataType.DJILocationCoordinate3D aircraftLocation = flightController.getCurrentState().getAircraftLocation();
                        Log.d(TAG, "current location: " + aircraftLocation.getLatitude() + ":" + aircraftLocation.getLongitude() + " " + aircraftLocation.getAltitude());
                        djiMission.addWaypoint(new DJIWaypoint(aircraftLocation.getLatitude(), aircraftLocation.getLongitude(), aircraftLocation.getAltitude()));
                    } else {
                        Log.w(TAG, "Flight controller null");
                    }
                } else {
                    Log.w(TAG, "product not aircraft");
                }

                djiMission.addWaypoint(new DJIWaypoint(target.latitude, target.longitude, 30));


                final DJIMissionManager missionManager = mapCopterManager.getProduct().getMissionManager();
                missionManager.prepareMission(djiMission, new DJIMission.DJIMissionProgressHandler() {
                    @Override
                    public void onProgress(DJIMission.DJIProgressType djiProgressType, float v) {
                        Log.d(TAG, "onProgress: mission progress " + v);

                    }
                }, new DJIBaseComponent.DJICompletionCallback() {
                    @Override
                    public void onResult(DJIError djiError) {
                        if (djiError != null) {
                            Log.e(TAG, "preparemission error: " + djiError.getDescription());
                        } else {
                            Log.d(TAG, "onResult: preparemission completed");
                            missionManager.startMissionExecution(new DJIBaseComponent.DJICompletionCallback() {
                                @Override
                                public void onResult(DJIError djiError) {
                                    if (djiError != null) {
                                        Log.e(TAG, "Start mission error: " + djiError.getDescription());
                                    } else {
                                        Log.d(TAG, "Start mission completed");
                                    }
                                }
                            });

                        }

                    }
                });

            }
        });

        mapCopterManager = MapCopterManager.createManager(this, this);
        mapCopterManager.initManager();

        IntentFilter filter = new IntentFilter();
        filter.addAction(FLAG_CONNECTION_CHANGE);
        registerReceiver(mReceiver, filter);
    }


    protected BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            onProductChange();
        }
    };

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

    private final Handler mHideHandler = new Handler();
    private final Runnable mHideRunnable = new Runnable() {
        @Override
        public void run() {
            hide();
        }
    };

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

    Marker marker;

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Add a marker and move the camera
        LatLng lipasto = new LatLng(65.0591, 25.466549);
        marker = mMap.addMarker(new MarkerOptions().position(lipasto).title("Lipasto"));
        aircraftLocationMarker = mMap.addMarker(new MarkerOptions().position(new LatLng(0, 0)));

        mMap.moveCamera(CameraUpdateFactory.newLatLng(lipasto));

        new MapStateListener(mMap, mapFragment, this) {
            @Override
            public void onMapTouched() {
                // Map touched
            }

            @Override
            public void onMapReleased() {
                Log.d(TAG, mMap.getCameraPosition().target.toString());
                marker.setPosition(mMap.getCameraPosition().target);

            }

            @Override
            public void onMapUnsettled() {
                // Map unsettled
            }

            @Override
            public void onMapSettled() {
                // Map settled
            }
        };
    }
}
