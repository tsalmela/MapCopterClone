package fi.oulu.mapcopter;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.util.Log;
import android.view.TextureView;

import dji.sdk.AirLink.DJILBAirLink;
import dji.sdk.Camera.DJICamera;
import dji.sdk.Codec.DJICodecManager;
import dji.sdk.base.DJIBaseProduct;

public class VideoSurfaceListener implements TextureView.SurfaceTextureListener, DJICamera.CameraReceivedVideoDataCallback, DJILBAirLink.DJIOnReceivedVideoCallback {
    private static final String TAG = VideoSurfaceListener.class.getSimpleName();


    private Context context;
    private MapCopterManager mapCopterManager;

    public VideoSurfaceListener(Context context, MapCopterManager mapCopterManager) {
        this.context = context;
        this.mapCopterManager = mapCopterManager;
    }

    private DJICodecManager mCodecManager;


    public void initPreviewer() {
        Log.d(TAG, "initPreviewer");
        DJIBaseProduct mProduct;
        try {
            mProduct = mapCopterManager.getProduct();
            Log.d(TAG, "initPreviewer: mProduct " + mProduct);
        } catch (Exception exception) {
            mProduct = null;
            Log.e(TAG, "initPreviewer: exception ", exception);
        }

        DJICamera mCamera;
        if (mProduct == null || !mProduct.isConnected()) {
            mCamera = null;
        } else {
            Log.d(TAG, "initPreviewer: product connected not null");
            //if (mCameraView != null) {
            //    mCameraView.setSurfaceTextureListener(surfaceListener);
            //}

            if (!mProduct.getModel().equals(DJIBaseProduct.Model.UnknownAircraft)) {
                mCamera = mProduct.getCamera();
                if (mCamera != null) {
                    mCamera.setDJICameraReceivedVideoDataCallback(this);
                }
            } else {
                if (mProduct.getAirLink() != null) {
                    if (mProduct.getAirLink().getLBAirLink() != null) {
                        mProduct.getAirLink().getLBAirLink().setDJIOnReceivedVideoCallback(this);
                    }
                }
            }
        }
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        if (mCodecManager == null) {
            Log.e(TAG, "codecmanager null");
            mCodecManager = new DJICodecManager(context, surface, width, height);
        }
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        Log.e(TAG, "onSurfaceTextureSizeChanged");
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        Log.e(TAG, "onSurfaceTextureDestroyed");
        if (mCodecManager != null) {
            mCodecManager.cleanSurface();
            mCodecManager = null;
        }
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        Log.e(TAG, "onSurfaceTextureUpdated: ");
    }

    @Override
    public void onResult(byte[] videoBuffer, int size) {
        Log.d(TAG, "got video");
        if (mCodecManager != null) {
            mCodecManager.sendDataToDecoder(videoBuffer, size);
        } else {
            Log.e(TAG, "codec manager null");

        }
    }
}
