package fi.oulu.mapcopter.copter;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;
import com.squareup.otto.Bus;

public abstract class CopterManager {
    private static final String TAG = CopterManager.class.getSimpleName();
    protected AircraftPositionChangeListener positionChangeListener;

    public abstract void initManager();
    
    public static CopterManager createManager(Context context, Bus eventBus) {
        String arch = System.getProperty("os.arch");
        Log.i(TAG, "DJICameraManager: cpu architecture: " + arch);
        // DJI sdk only works on armeabi cpus, so we create a dummy manager
        // to avoid crashing on emulators or other non-armeabi devices
        if (arch.startsWith("armv")) {
            return new DJICopterManager(context, eventBus);
        } else {
            Log.w(TAG, "DJI sdk requires armeabiv7 cpu, using dummy CopterManager.");
            return new DummyCopterManager();
        }
    }

    public abstract @NonNull CameraManager getCameraManager();

    public abstract void moveToPos(double latitude, double longitude);

    @NonNull
    public abstract LatLng getCurrentPosition();

    public void setCopterPositionChangeListener(AircraftPositionChangeListener positionChangeListener) {
        this.positionChangeListener = positionChangeListener;
    }
}
