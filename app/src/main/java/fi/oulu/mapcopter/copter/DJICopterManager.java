package fi.oulu.mapcopter.copter;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.util.Log;

import dji.sdk.FlightController.DJICompass;
import dji.sdk.FlightController.DJIFlightController;

import com.google.android.gms.maps.model.LatLng;
import com.squareup.otto.Bus;

import dji.sdk.FlightController.DJIFlightControllerDataType;
import dji.sdk.FlightController.DJIFlightControllerDelegate;
import dji.sdk.MissionManager.DJICustomMission;
import dji.sdk.MissionManager.DJIMission;
import dji.sdk.MissionManager.DJIMissionManager;
import dji.sdk.MissionManager.DJIWaypoint;
import dji.sdk.MissionManager.DJIWaypointMission;
import dji.sdk.MissionManager.MissionStep.DJIGoToStep;
import dji.sdk.Products.DJIAircraft;
import dji.sdk.SDKManager.DJISDKManager;
import dji.sdk.base.DJIBaseComponent;
import dji.sdk.base.DJIBaseProduct;
import dji.sdk.base.DJIError;
import dji.sdk.base.DJISDKError;
import fi.oulu.mapcopter.event.CopterConnectionEvent;
import fi.oulu.mapcopter.event.CopterStatusChangeEvent;

public class DJICopterManager extends CopterManager implements DJISDKManager.DJISDKManagerCallback, DJIMission.DJIMissionProgressHandler {
    private static final String TAG = DJICopterManager.class.getSimpleName();

    /**
     * The mission speed can be changed with the control stick
     * in the remote controller up to this maximum speed
     */
    public static final int MISSION_MAX_FLIGHT_SPEED = 7;

    /**
     * The starting speed for the waypoint missions
     */
    public static final int MISSION_FLIGHT_SPEED = 1;

    private final Bus eventBus;
    private Context context;
    private Handler mainThreadHandler;

    private DJIBaseProduct mProduct;
    private DJIFlightController flightController;
    private DJIAircraft aircraft;

    private float missionAltitude;

    private final DJICameraManager cameraManager;

    public DJICopterManager(final Context context, final Bus eventBus) {
        this.eventBus = eventBus;
        this.context = context;
        mainThreadHandler = new Handler(context.getMainLooper());
//        this.cameraManager = new DJICameraManager();
        this.cameraManager = new DJICameraManager();
    }

    @Override
    public void initManager() {
        DJISDKManager.getInstance()
                .initSDKManager(context, this);
    }

    public void startCompassCalibration() {
        if (flightController != null) {
            flightController.getCompass().startCompassCalibration(new DJIBaseComponent.DJICompletionCallback() {
                @Override
                public void onResult(DJIError djiError) {
                    Log.d(TAG, "Calibration started ");
                    if (djiError != null) {
                        Log.e(TAG, "Error during calibration: " + djiError.getDescription());
                    }
                    DJICompass.DJICompassCalibrationStatus calibrationStatus = flightController.getCompass().getCalibrationStatus();
                    Log.d(TAG, "Calibration status: " + calibrationStatus.name());
                }
            });
        }
    }

    public int getCompassStatus() {
        if (flightController != null) {
            return flightController.getCompass().getCalibrationStatus().value();
        } else {
            return -1;
        }
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
//            Log.w(TAG, "getCurrentPosition called but flight controller is null");
            return new LatLng(0, 0);
        }
    }

    @Override
    public void stopCompassCalibration() {
        if (flightController != null) {
            flightController.getCompass().stopCompassCalibration(new DJIBaseComponent.DJICompletionCallback() {
                @Override
                public void onResult(DJIError djiError) {
                    Log.d(TAG, "Stopped calibration");
                    if (djiError != null) {
                        Log.e(TAG, "Error stopping calibration: " + djiError);
                    }
                    Log.d(TAG, "Calibration status after stop: " + flightController.getCompass().getCalibrationStatus().name());
                }
            });
        }
    }


    @Override
    public void getHomePosition(final HomePositionCallback callback) {
        if (flightController != null) {
            flightController.getHomeLocation(new DJIBaseComponent.DJICompletionCallbackWith<DJIFlightControllerDataType.DJILocationCoordinate2D>() {
                @Override
                public void onSuccess(DJIFlightControllerDataType.DJILocationCoordinate2D location) {
                    if (callback != null) {
                        callback.onSuccess(location.getLatitude(), location.getLongitude());
                    }
                }

                @Override
                public void onFailure(DJIError djiError) {

                }
            });
        }
    }

    @Override
    public void setAltitude(float altitude) {
        Log.d(TAG, "Setting altitude to " + altitude);
        missionAltitude = altitude;
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
                        //Log.d(TAG, "Head direction: " + direction);

                        if (direction < 0) {
                            direction = 3600 + direction;
                        }

                        //Log.d(TAG, "Real direction: " + String.format("%.1f", (float) direction / 10));

                        if (positionChangeListener != null) {
                            // ensure that the callback is ran in the UI thread
                            if (Thread.currentThread() == Looper.getMainLooper().getThread()) {
                                DJIFlightControllerDataType.DJILocationCoordinate3D location = state.getAircraftLocation();
                                positionChangeListener.onAircraftPositionChanged(location.getLatitude(), location.getLongitude(), location.getAltitude(), direction);
                            } else {
                                final double directionFinal = direction;
                                mainThreadHandler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        DJIFlightControllerDataType.DJILocationCoordinate3D location = state.getAircraftLocation();
                                        positionChangeListener.onAircraftPositionChanged(location.getLatitude(), location.getLongitude(), location.getAltitude(), directionFinal);
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
                        if (callback != null) {
                            callback.run();
                        }
                    }
                });
            }
        }
    }

    @Override
    public void takeOff() {
        if (isConnected()) {
            aircraft.getFlightController().takeOff(new DJIBaseComponent.DJICompletionCallback() {
                @Override
                public void onResult(DJIError error) {
                    if (error == null) {
                        Log.d(TAG, "Take off success");
                    } else {
                        eventBus.post(new CopterStatusChangeEvent("Take off failed: " + error.getDescription()));
                    }
                }
            });
        }
    }


    @Override
    public void moveToPos(final double latitude, final double longitude) {
        final DJIWaypointMission mission = new DJIWaypointMission();
        mission.maxFlightSpeed = MISSION_MAX_FLIGHT_SPEED;
        mission.autoFlightSpeed = MISSION_FLIGHT_SPEED;

        final LatLng currentPosition = getCurrentPosition();

        stopMission(new Runnable() {
            @Override
            public void run() {
                float altitude;
                if (missionAltitude <= 0) {
                    altitude = getCurrentAltitude();
                } else {
                    altitude = missionAltitude;
                }
                mission.addWaypoint(new DJIWaypoint(currentPosition.latitude, currentPosition.longitude, altitude));
                mission.addWaypoint(new DJIWaypoint(latitude, longitude, altitude));

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
    public void stopCopter() {
        stopMission(null);

    }

    @Override
    public int getGPSStatus() {
        if (flightController != null){
            DJIFlightControllerDataType.DJIGPSSignalStatus gpsSignalStatus = flightController.getCurrentState().getGpsSignalStatus();

            return gpsSignalStatus.value();
        }
        return DJIFlightControllerDataType.DJIGPSSignalStatus.None.value();
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

