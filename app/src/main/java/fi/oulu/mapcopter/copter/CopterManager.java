package fi.oulu.mapcopter.copter;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;
import com.squareup.otto.Bus;

/**
 * Abstraction class for controlling and accessing the copter
 */
public abstract class CopterManager {
    private static final String TAG = CopterManager.class.getSimpleName();
    protected AircraftPositionChangeListener positionChangeListener;

    public abstract void initManager();

    public abstract int getCompassStatus();

    public abstract void startCompassCalibration();

    public abstract void takeOff();

    /**
     * Static factory method to create the concrete CopterManager class.
     * Creates the dummy manager on architectures not supported by the DJI SDK to
     * avoid crashing.
     *
     * <br><br>
     * TODO: fix, removed architecture check because of incorrect detection on some devices. Also,
     * possibly not required if we update to DJI SDK 3.2 which includes support for more architectures.
     *
     */
    public static CopterManager createManager(Context context, Bus eventBus) {
        String arch = System.getProperty("os.arch");
        Log.i(TAG, "DJICameraManager: cpu architecture: " + arch);
        // DJI sdk only works on armeabi cpus, so we create a dummy manager
        // to avoid crashing on emulators or other non-armeabi devices
//        if (arch.startsWith("armv")) {
            return new DJICopterManager(context, eventBus);
//        } else {
//            Log.w(TAG, "DJI sdk requires armeabiv7 cpu, using dummy CopterManager.");
//            return new DummyCopterManager();
//        }
    }

    @NonNull
    public abstract CameraManager getCameraManager();

    /**
     * Order the copter to move to the specified coordinates
     * @param latitude latitude
     * @param longitude longitude
     */
    public abstract void moveToPos(double latitude, double longitude);

    /**
     * Order the copter to stop the current mission.
     */
    public abstract void stopCopter();

    public abstract int getGPSStatus();

    /**
     * Set the altitude for the <strong>next mission.</strong>
     * Does not change the altitude immediately. Only after a new mission is given
     * with {@link #moveToPos(double, double)}
     *
     * @param altitude altitude in meters
     */
    public abstract void setAltitude(float altitude);

    /**
     * Returns the current position coordinates of the aircraft.
     * @return LatLng current coordinates
     */
    @NonNull
    public abstract LatLng getCurrentPosition();

    /**
     * Set the listener to be called every time the aircraft position/altitude changes
     * @param positionChangeListener change listener
     */
    public void setCopterPositionChangeListener(AircraftPositionChangeListener positionChangeListener) {
        this.positionChangeListener = positionChangeListener;
    }

    public abstract void stopCompassCalibration();

    /**
     * Calls the callback with the home/starting position of the aircraft.
     * @param callback callback for receiving the result
     */
    public abstract void getHomePositionAsync(final DJICopterManager.HomePositionCallback callback);


    /**
     * used by {@link #getHomePositionAsync(HomePositionCallback)}
     */
    public interface HomePositionCallback {
        void onSuccess(double latitude, double longitude);
    }
}
