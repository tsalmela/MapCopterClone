package fi.oulu.mapcopter;

import android.content.Context;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;
import com.squareup.otto.Bus;

import dji.sdk.Codec.DJICodecManager;
import dji.sdk.FlightController.DJIFlightController;
import dji.sdk.base.DJIBaseProduct;

public abstract class MapCopterManager {
    private static final String TAG = MapCopterManager.class.getSimpleName();

    public abstract DJIBaseProduct getProduct();

    public abstract void initManager();
    
    public static MapCopterManager createManager(Context context, Bus eventBus) {
        String arch = System.getProperty("os.arch");
        Log.i(TAG, "CodecManagerProxy: cpu architecture: " + arch);
        // DJI sdk only works on armeabi cpus, so we create a dummy manager
        // to avoid crashing on emulators or other non-armeabi devices
        if (arch.startsWith("armv")) {
            return new MapCopterRealManager(context, eventBus);
        } else {
            Log.w(TAG, "DJI sdk requires armeabiv7 cpu, using dummy MapCopterManager.");
            return new MapCopterDummyManager();
        }
    }

    public abstract DJIFlightController getFlightController();

    public abstract void moveToPos(LatLng position);
}
