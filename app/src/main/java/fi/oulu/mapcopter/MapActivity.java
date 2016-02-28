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
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;

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

    private CopterManager mapCopterManager;
    private GoogleMap mMap;
    private Marker aircraftLocationMarker;
    private Marker destinationMarker;

    private Bus eventBus;

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
        Log.d(TAG, "Current aircraft position: " + mMap.getCameraPosition().target.toString());

        LatLng target = mMap.getCameraPosition().target;
        destinationMarker.setPosition(target);
        mapCopterManager.moveToPos(target.latitude, target.longitude);
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
    }

    private void prepareMap() {
        TouchableMapFragment mapFragment = (TouchableMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.setTouchListener(new TouchableMapFragment.TouchListener() {
            @Override
            public void onTouchStart() {}

            @Override
            public void onTouchEnd() {
                onMapTouchEnd();
            }
        });
        mapFragment.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(GoogleMap googleMap) {
                mMap = googleMap;
                LatLng lipasto = new LatLng(65.0591, 25.466549);

                destinationMarker = mMap.addMarker(new MarkerOptions()
                        .position(new LatLng(0, 0))
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)));
                aircraftLocationMarker = mMap.addMarker(new MarkerOptions()
                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.icon_aircraft))
                        .position(new LatLng(0, 0)));

                mMap.moveCamera(CameraUpdateFactory.newLatLng(lipasto));
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

    @Override
    public void onAircraftPositionChanged(double latitude, double longitude, float altitude) {
        if (aircraftLocationMarker != null) {
            aircraftLocationMarker.setPosition(new LatLng(latitude, longitude));
        }
    }
}
