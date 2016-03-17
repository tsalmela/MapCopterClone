package fi.oulu.mapcopter.copter;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.util.Log;

import dji.sdk.FlightController.DJIFlightController;

import com.google.android.gms.maps.model.LatLng;
import com.squareup.otto.Bus;

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
import dji.sdk.base.DJISDKError;
import fi.oulu.mapcopter.event.CopterConnectionEvent;
import fi.oulu.mapcopter.event.CopterStatusChangeEvent;

public class DJICopterManager extends CopterManager implements DJISDKManager.DJISDKManagerCallback, DJIMission.DJIMissionProgressHandler  {
    private static final String TAG = DJICopterManager.class.getSimpleName();

    private final Bus eventBus;
    private Context context;
    private Handler mainThreadHandler;

    private DJIBaseProduct mProduct;
    private DJIFlightController flightController;
    private DJIAircraft aircraft;

    private final DJICameraManager cameraManager;

    public DJICopterManager(final Context context, final Bus eventBus) {
        this.eventBus = eventBus;
        this.context = context;
        mainThreadHandler = new Handler(context.getMainLooper());
        this.cameraManager = new DJICameraManager();
    }

    @Override
    public void initManager() {
        DJISDKManager.getInstance()
                .initSDKManager(context, this);
    }

    @NonNull
    @Override
    public CameraManager getCameraManager() {
        return cameraManager;
    }

    @NonNull
    @Override
    public LatLng getCurrentPosition() {
        if (flightController != null) {
            DJIFlightControllerDataType.DJILocationCoordinate3D aircraftLocation = flightController.getCurrentState().getAircraftLocation();
            return new LatLng(aircraftLocation.getLatitude(), aircraftLocation.getLongitude());
        } else {
            Log.w(TAG, "getCurrentPosition called but flight controller is null");
            return new LatLng(0, 0);
        }
    }

    private float getCurrentAltitude() {
        if (flightController != null) {
            DJIFlightControllerDataType.DJILocationCoordinate3D aircraftLocation = flightController.getCurrentState().getAircraftLocation();
            return aircraftLocation.getAltitude();
        } else {
            Log.w(TAG, "getCurrentAltitude called but flight controller is null");
            return 0;
        }
    }

    private void initProduct(DJIBaseProduct product) {
        if (product instanceof DJIAircraft) {
            aircraft = (DJIAircraft) mProduct;
            flightController = aircraft.getFlightController();

            if (flightController != null) {
                flightController.setUpdateSystemStateCallback(new DJIFlightControllerDelegate.FlightControllerUpdateSystemStateCallback() {
                    @Override
                    public void onResult(final DJIFlightControllerDataType.DJIFlightControllerCurrentState state) {
                        int direction = state.getAircraftHeadDirection();
                        Log.d(TAG, "Head direction: " + direction);

                        if (direction < 0) {
                            direction = 3600 + direction;
                        }

                        Log.d(TAG, "Real direction: " + String.format("%.1f", (float) direction / 10));

                        if (positionChangeListener != null) {
                            // ensure that the callback is ran in the UI thread
                            if (Thread.currentThread() == Looper.getMainLooper().getThread()) {
                                DJIFlightControllerDataType.DJILocationCoordinate3D location = state.getAircraftLocation();
                                positionChangeListener.onAircraftPositionChanged(location.getLatitude(), location.getLongitude(), location.getAltitude());
                            } else {
                                mainThreadHandler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        DJIFlightControllerDataType.DJILocationCoordinate3D location = state.getAircraftLocation();
                                        positionChangeListener.onAircraftPositionChanged(location.getLatitude(), location.getLongitude(), location.getAltitude());
                                    }
                                });
                            }
                        }
                    }
                });
            }
            if (aircraft.getCamera() != null) {
                cameraManager.initCamera(aircraft.getCamera(), product);
            } else {
                Log.w(TAG, "initProduct: no camera available");
            }
        } else {
            Log.e(TAG, "initProduct failed, product is " + product);
        }
    }

    private boolean isConnected() {
        return mProduct != null && mProduct.isConnected();
    }


    private void stopMission(final Runnable callback) {
        if (mProduct != null && mProduct.isConnected()) {
            DJIMissionManager missionManager = mProduct.getMissionManager();
            if (missionManager != null && missionManager.isConnected()) {
                missionManager.stopMissionExecution(new DJIBaseComponent.DJICompletionCallback() {
                    @Override
                    public void onResult(DJIError error) {
                        if (error != null) {
                            eventBus.post(new CopterStatusChangeEvent("Failed to stop mission: " + error.getDescription()));
                        } else {
                            eventBus.post(new CopterStatusChangeEvent("Stopped mission"));
                        }
                        callback.run();
                    }
                });
            }
        }
    }



    @Override
    public void moveToPos(final double latitude, final double longitude) {
        final DJIWaypointMission mission = new DJIWaypointMission();
        mission.maxFlightSpeed = 15;
        mission.autoFlightSpeed = 15;

        final LatLng currentPosition = getCurrentPosition();

        stopMission(new Runnable() {
            @Override
            public void run() {
                mission.addWaypoint(new DJIWaypoint(currentPosition.latitude, currentPosition.longitude, getCurrentAltitude()));
                mission.addWaypoint(new DJIWaypoint(latitude, longitude, getCurrentAltitude()));

                mProduct.getMissionManager().prepareMission(mission, DJICopterManager.this, new DJIBaseComponent.DJICompletionCallback() {
                    @Override
                    public void onResult(DJIError error) {
                        if (error != null) {
                            Log.e(TAG, "preparemission error: " + error.getDescription());
                            eventBus.post(new CopterStatusChangeEvent("Preparemission failed: " + error.getDescription()));
                        } else {
                            Log.d(TAG, "onResult: preparemission completed");
                            if (isConnected()) {
                                mProduct.getMissionManager().startMissionExecution(new DJIBaseComponent.DJICompletionCallback() {
                                    @Override
                                    public void onResult(DJIError error) {
                                        if (error != null) {
                                            eventBus.post(new CopterStatusChangeEvent("Start mission failed: " + error.getDescription()));
                                            Log.e(TAG, "Start mission error: " + error.getDescription());
                                        } else {
                                            eventBus.post(new CopterStatusChangeEvent("Mission started"));
                                            Log.d(TAG, "Start mission completed");
                                        }
                                    }
                                });
                            }
                        }
                    }
                });

            }
        });
    }

    @Override
    public void onGetRegisteredResult(DJIError error) {
        if (error == DJISDKError.REGISTRATION_SUCCESS) {
            Log.i(TAG, "onGetRegisteredResult registration success");
            boolean connectionSucceeded = DJISDKManager.getInstance().startConnectionToProduct();
            if (!connectionSucceeded) {
                eventBus.post(new CopterStatusChangeEvent("Failed to connect to product"));
            }
        } else {
            Log.e(TAG, "onGetRegisteredResult: registration not success " + error.getDescription());
            eventBus.post(new CopterStatusChangeEvent("DJI App registration failed " + error.getDescription()));
        }
    }

    @Override
    public void onProductChanged(DJIBaseProduct oldProduct, DJIBaseProduct newProduct) {
        mProduct = newProduct;
        Log.d(TAG, "onProductChanged");
        boolean isConnected = false;
        if (mProduct != null) {
            isConnected = true;
            initProduct(mProduct);
            mProduct.setDJIBaseProductListener(mDJIBaseProductListener);
            eventBus.post(new CopterStatusChangeEvent("Product connected: " + mProduct.getModel().getDisplayName()));
        } else {
            Log.w(TAG, "onProductChange new product is null");
            eventBus.post(new CopterStatusChangeEvent("Product disconnected"));
        }
        eventBus.post(new CopterConnectionEvent(isConnected));
    }

    private DJIBaseProduct.DJIBaseProductListener mDJIBaseProductListener = new DJIBaseProduct.DJIBaseProductListener() {
        @Override
        public void onComponentChange(final DJIBaseProduct.DJIComponentKey key, DJIBaseComponent oldComponent, DJIBaseComponent newComponent) {
            if (newComponent != null) {
                Log.d(TAG, "Component: " + key.name() + " connected");
                newComponent.setDJIComponentListener(new DJIBaseComponent.DJIComponentListener() {
                    @Override
                    public void onComponentConnectivityChanged(boolean isConnected) {
                        Log.d(TAG, "Connectivity changed for component " + key.name() + " to " + isConnected);
                    }
                });
            } else {
                Log.w(TAG, "Component: " + key.name() + " disconnected");
            }
        }

        @Override
        public void onProductConnectivityChanged(boolean isConnected) {
            Log.d(TAG, "onProductConnectivityChanged to " + isConnected);
            // TODO: notify
        }
    };

    /**
     * Mission progress handler for prepare mission
     */
    @Override
    public void onProgress(DJIMission.DJIProgressType djiProgressType, float progress) {
        Log.d(TAG, "onProgress: mission progress " + progress + " for type " + djiProgressType.name());
    }
}

