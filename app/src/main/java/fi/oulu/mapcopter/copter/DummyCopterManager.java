package fi.oulu.mapcopter.copter;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.support.annotation.NonNull;
import android.util.Log;

public class DummyCopterManager extends CopterManager {
    private static final String TAG = DummyCopterManager.class.getSimpleName();

    @Override
    public void initManager() {}

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
}
