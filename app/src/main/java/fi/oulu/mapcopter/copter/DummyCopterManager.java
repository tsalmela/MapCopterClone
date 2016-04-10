package fi.oulu.mapcopter.copter;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;

public class DummyCopterManager extends CopterManager {
    private static final String TAG = DummyCopterManager.class.getSimpleName();

    @Override
    public void initManager() {}

    @Override
    public int getCompassStatus() {
        return 0;
    }

    @Override
    public void startCompassCalibration() {
        Log.d(TAG, "Dummy calibration start called");
    }

    @Override
    public void takeOff() {
        Log.d(TAG, "Dummy take off called");
    }

    @NonNull
    @Override
    public CameraManager getCameraManager() {
        return new CameraManager() {
            @Override
            public void initSurface(Context context, SurfaceTexture surface, int width, int height) {}

            @Override
            public void cleanSurface() {}
        };
    }

    @Override
    public void moveToPos(double latitude, double longitude) {
        Log.d(TAG, "dummy moveToPos: " + latitude + ":" + longitude);
    }

    @Override
    public void stopCopter() {
        Log.d(TAG, "dummy stopCopter");
    }

    @Override
    public int getGPSStatus() {
        return 0;
    }

    @Override
    public void setAltitude(float altitude) {
        Log.d(TAG, "Dummy altitude to " + altitude);
    }

    @NonNull
    @Override
    public LatLng getCurrentPosition() {
        return new LatLng(0, 0);
    }

    @Override
    public void stopCompassCalibration() {

    }
}
