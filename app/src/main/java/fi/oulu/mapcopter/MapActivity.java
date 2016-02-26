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
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;

import dji.sdk.Camera.DJICamera;
import dji.sdk.FlightController.DJIFlightController;
import dji.sdk.FlightController.DJIFlightControllerDataType;
import dji.sdk.FlightController.DJIFlightControllerDelegate;
import dji.sdk.MissionManager.DJIMission;
import dji.sdk.MissionManager.DJIMissionManager;
import dji.sdk.MissionManager.DJIWaypoint;
import dji.sdk.MissionManager.DJIWaypointMission;
import dji.sdk.Products.DJIAircraft;
import dji.sdk.base.DJIBaseComponent;
import dji.sdk.base.DJIBaseProduct;
import dji.sdk.base.DJIError;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class MapActivity extends AppCompatActivity implements OnMapReadyCallback {
    private static final String TAG = MapActivity.class.getSimpleName();

    private static final int UI_ANIMATION_DELAY = 500;

    private TextureView mCameraView;
    private MapCopterManager mapCopterManager;
    private VideoSurfaceListener surfaceListener;
    private GoogleMap mMap;
    private Marker marker;
    private Button mStopButton;
    private TouchableMapFragment mapFragment;
    private Marker aircraftLocationMarker;
    private Marker destinationMarker;

    private Bus eventBus;


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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

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
                djiMission.maxFlightSpeed = 15;
                djiMission.autoFlightSpeed = 15;
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
                destinationMarker.setPosition(target);

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

        eventBus = MapCopter.getDefaultBus();
        mapCopterManager = MapCopter.getMapCopterManager();
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

    @Subscribe
    public void onCopterStatusChanged(CopterStatusChangeEvent event) {
        Toast.makeText(this, event.getMessage(), Toast.LENGTH_LONG).show();
    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Add a marker and move the camera
        LatLng lipasto = new LatLng(65.0591, 25.466549);
        marker = mMap.addMarker(new MarkerOptions().position(lipasto).title("Lipasto"));
        destinationMarker = mMap.addMarker(new MarkerOptions()
                .position(new LatLng(0, 0))
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)));
        aircraftLocationMarker = mMap.addMarker(new MarkerOptions()
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.icon_aircraft))
                .position(new LatLng(0, 0)));

        mMap.moveCamera(CameraUpdateFactory.newLatLng(lipasto));

        new MapStateListener(mMap, mapFragment, this) {
            @Override
            public void onMapTouched() {
                // Map touched
            }

            @Override
            public void onMapReleased() {
                LatLng centerOfMap = mMap.getCameraPosition().target;
                Log.d(TAG, centerOfMap.toString());
                marker.setPosition(centerOfMap);
                mapCopterManager.moveToPos(centerOfMap);
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
