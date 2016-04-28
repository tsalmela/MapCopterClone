package fi.oulu.mapcopter.copter;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.support.annotation.NonNull;
import android.util.Log;

import java.io.IOException;

import dji.sdk.AirLink.DJILBAirLink;
import dji.sdk.Camera.DJICamera;
import dji.sdk.Camera.DJICameraSettingsDef;
import dji.sdk.Codec.DJICodecManager;
import dji.sdk.base.DJIBaseComponent;
import dji.sdk.base.DJIBaseProduct;
import dji.sdk.base.DJIError;

public class DJICameraManager implements DJICamera.CameraReceivedVideoDataCallback, DJILBAirLink.DJIOnReceivedVideoCallback, CameraManager {
    private static final String TAG = DJICameraManager.class.getSimpleName();

    private DJICodecManager codecManager;

    public void initCamera(@NonNull DJICamera camera, @NonNull DJIBaseProduct product) {
        camera.setDJICameraReceivedVideoDataCallback(this);
        if (product.getAirLink() != null) {
            product.getCamera().setVideoResolutionAndFrameRate(DJICameraSettingsDef.CameraVideoResolution.Resolution_1920x1080, DJICameraSettingsDef.CameraVideoFrameRate.FrameRate_30fps, null);
            
            product.getCamera().setPhotoRatio(DJICameraSettingsDef.CameraPhotoAspectRatio.AspectRatio_16_9, null);
            if (product.getAirLink().getLBAirLink() != null) {
                product.getAirLink().getLBAirLink().setDJIOnReceivedVideoCallback(this);
            }
        }
    }

    @Override
    public void initSurface(Context context, SurfaceTexture surface, int width, int height) {
        if (codecManager != null) {
            Log.w(TAG, "initSurface called after codec manager already initialized");
            codecManager.cleanSurface();
        }
        String arch = System.getProperty("os.arch");
        if (arch.startsWith("armv")) {
            Log.i(TAG, "Initializing codec manager: cpu architecture: " + arch);
            this.codecManager = new DJICodecManager(context, surface, width, height);
        } else {
            Log.e(TAG, "initSurface: codec manager not initialized, incompatible cpu architecture " + arch);
        }
    }

    @Override
    public void cleanSurface() {
        if (codecManager != null) {
            codecManager.cleanSurface();
            codecManager = null;
        }
    }

    @Override
    public void onResult(byte[] videoBuffer, int size) {
        if (codecManager != null) {
            codecManager.sendDataToDecoder(videoBuffer, size);
        }
    }
}
