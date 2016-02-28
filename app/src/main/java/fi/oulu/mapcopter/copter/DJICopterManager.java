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

public class DJICopterManager extends CopterManager implements DJISDKManager.DJISDKManagerCallback {
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

    private LatLng getCurrentPosition() {
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

    @Override
    public void moveToPos(double latitude, double longitude) {
        DJIWaypointMission mission = new DJIWaypointMission();
        mission.maxFlightSpeed = 15;
        mission.autoFlightSpeed = 15;

        LatLng currentPosition = getCurrentPosition();

        mission.addWaypoint(new DJIWaypoint(currentPosition.latitude, currentPosition.longitude, getCurrentAltitude()));

        mission.addWaypoint(new DJIWaypoint(latitude, longitude, getCurrentAltitude()));

        mProduct.getMissionManager().prepareMission(mission, new DJIMission.DJIMissionProgressHandler() {
            @Override
            public void onProgress(DJIMission.DJIProgressType djiProgressType, float v) {
                Log.d(TAG, "onProgress: mission progress " + v);

            }
        }, new DJIBaseComponent.DJICompletionCallback() {
            @Override
            public void onResult(DJIError error) {
                if (error != null) {
                    Log.e(TAG, "preparemission error: " + error.getDescription());
                } else {
                    Log.d(TAG, "onResult: preparemission completed");
                    if (isConnected()) {
                        mProduct.getMissionManager().startMissionExecution(new DJIBaseComponent.DJICompletionCallback() {
                            @Override
                            public void onResult(DJIError error) {
                                if (error != null) {
                                    Log.e(TAG, "Start mission error: " + error.getDescription());
                                } else {
                                    Log.d(TAG, "Start mission completed");
                                }
                            }
                        });
                    }
                }
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

}

