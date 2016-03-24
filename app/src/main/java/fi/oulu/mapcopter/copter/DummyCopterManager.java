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
        Log.d(TAG, "moveToPos: " + latitude + ":" + longitude);
    }

    @Override
    public void stopCopter() {
        Log.d(TAG, "stopping");
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
