package fi.oulu.mapcopter.remote.view;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.util.Log;
import android.view.TextureView;

import fi.oulu.mapcopter.remote.CameraManager;


public class VideoSurfaceListener implements TextureView.SurfaceTextureListener {
    private static final String TAG = VideoSurfaceListener.class.getSimpleName();

    private Context context;
    private CameraManager cameraManager;

    public VideoSurfaceListener(Context context, CameraManager cameraManager) {
        this.context = context;
        this.cameraManager = cameraManager;
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        Log.i(TAG, "onSurfaceTextureAvailable size " + width + ":" + height);
        if (cameraManager != null) {
            cameraManager.initSurface(context, surface, width, height);
        }
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        Log.i(TAG, "onSurfaceTextureSizeChanged");
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        Log.i(TAG, "onSurfaceTextureDestroyed");
        if (cameraManager != null) {
            cameraManager.cleanSurface();
        }
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
    }
}
