package fi.oulu.mapcopter;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.util.Log;

import dji.sdk.Codec.DJICodecManager;

public class CodecManagerProxy {
    private static final String TAG = CodecManagerProxy.class.getSimpleName();

    private DJICodecManager codecManager;

    public CodecManagerProxy(DJICodecManager codecManager) {
        this.codecManager = codecManager;
    }

    public CodecManagerProxy(Context context, SurfaceTexture surface, int width, int height) {
        String arch = System.getProperty("os.arch");
        Log.i(TAG, "CodecManagerProxy: cpu architecture: " + arch);
        if (arch.startsWith("armv")) {
            this.codecManager = new DJICodecManager(context, surface, width, height);
        }
    }

    void cleanSurface() {
        if (codecManager != null) {
            codecManager.cleanSurface();
        }
    }

    public void sendDataToDecoder(byte[] videoBuffer, int size) {
        if (codecManager != null) {
            codecManager.sendDataToDecoder(videoBuffer, size);
        }
    }
}
