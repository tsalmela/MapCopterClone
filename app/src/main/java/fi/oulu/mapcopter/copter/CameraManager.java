package fi.oulu.mapcopter.copter;

import android.content.Context;
import android.graphics.SurfaceTexture;

public interface CameraManager {
    void initSurface(Context context, SurfaceTexture surface, int width, int height);

    void cleanSurface();
}
