package fi.oulu.mapcopter.copter;

import android.content.Context;
import android.graphics.SurfaceTexture;

/**
 * Used to display the camera preview from the copter inside a {@link android.view.TextureView}
 *
 * See {@link fi.oulu.mapcopter.view.VideoSurfaceListener}
 */
public interface CameraManager {

    /**
     * Should be called when the surface texture is ready to use.
     * @param context context
     * @param surface surface
     * @param width width in pixels of the surface
     * @param height height in pixels of the surface
     */
    void initSurface(Context context, SurfaceTexture surface, int width, int height);

    void cleanSurface();
}
