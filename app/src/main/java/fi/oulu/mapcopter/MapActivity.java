package fi.oulu.mapcopter;

import android.os.Bundle;
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
import fi.oulu.mapcopter.copter.CopterManager;
import fi.oulu.mapcopter.event.CopterConnectionEvent;
import fi.oulu.mapcopter.event.CopterStatusChangeEvent;
import fi.oulu.mapcopter.view.TouchableMapFragment;
import fi.oulu.mapcopter.view.VideoSurfaceListener;

public class MapActivity extends AppCompatActivity implements AircraftPositionChangeListener {
    private static final String TAG = MapActivity.class.getSimpleName();
    public static final double MAP_BOUNDS_LIMIT_LATITUDE = 0.0035;
    public static final double MAP_BOUNDS_LIMIT_LONGITUDE = 0.01;

    private CopterManager mapCopterManager;
    private GoogleMap mMap;
    private Marker aircraftLocationMarker;
    private Marker destinationMarker;

    private Bus eventBus;
    private Polygon boundsPolygon;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);
        ButterKnife.bind(this);

        prepareMap();

        eventBus = MapCopterApplication.getDefaultBus();
        mapCopterManager = MapCopterApplication.getCopterManager();
        mapCopterManager.setCopterPositionChangeListener(this);

        TextureView mCameraView = ButterKnife.findById(this, R.id.camera_view);
        VideoSurfaceListener surfaceListener = new VideoSurfaceListener(this, mapCopterManager.getCameraManager());
        mCameraView.setSurfaceTextureListener(surfaceListener);
    }

    @OnClick(R.id.button_stop)
    public void onStopButtonClicked() {
        mapCopterManager.stopCopter();
        LatLng target = mMap.getCameraPosition().target;
        onAircraftPositionChanged(target.latitude, target.longitude, 0, 0);
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

                mMap.moveCamera(CameraUpdateFactory.newLatLng(lipasto));

                mMap.setOnCameraChangeListener(new GoogleMap.OnCameraChangeListener() {
                    @Override
                    public void onCameraChange(CameraPosition cameraPosition) {
                        //LatLng currentPosition = mapCopterManager.getCurrentPosition();
                        LatLng currentPosition = lipasto;
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
