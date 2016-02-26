package fi.oulu.mapcopter;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.util.Log;

import dji.sdk.FlightController.DJIFlightController;
import dji.sdk.SDKManager.DJISDKManager;
import dji.sdk.base.DJIBaseComponent;
import dji.sdk.base.DJIBaseProduct;
import dji.sdk.base.DJIError;
import dji.sdk.base.DJISDKError;

public class MapCopterRealManager extends MapCopterManager implements DJISDKManager.DJISDKManagerCallback {
    private static final String TAG = MapCopterRealManager.class.getSimpleName();

    private final Context context;
    private DJIBaseProduct mProduct;
    private CopterStatusChangeListener statusListener;

    public MapCopterRealManager(Context context, CopterStatusChangeListener statusListener) {
        this.context = context;
        this.statusListener = statusListener;
    }

    @Override
    public DJIBaseProduct getProduct() {
        if (mProduct == null) {
            Log.d(TAG, "getProductInstance");
            mProduct = DJISDKManager.getInstance().getDJIProduct();
        } else {
            Log.d(TAG, "getProductInstance: else");
        }
        return mProduct;
    }

    @Override
    public void initManager() {
        DJISDKManager.getInstance()
                .initSDKManager(context, this);
    }

    @Override
    public void onGetRegisteredResult(DJIError error) {
        if (error == DJISDKError.REGISTRATION_SUCCESS) {
            Log.i(TAG, "onGetRegisteredResult registration success");
            DJISDKManager.getInstance().startConnectionToProduct();
        } else {
            Log.e(TAG, "onGetRegisteredResult: registration not success");
            Handler handler = new Handler(Looper.getMainLooper());
            handler.post(new Runnable() {
                @Override
                public void run() {
                    // TODO: display error
                    //Toast.makeText(getApplicationContext(),
                    //        "DJI SDK registration failed, check network connection",
                    //        Toast.LENGTH_LONG)
                    //        .show();
                }
            });
            Log.e(TAG, "onGetRegisteredResult" + error.toString());
            Log.e(TAG, "Error description: " + error.getDescription());
        }
    }

    @Override
    public void onProductChanged(DJIBaseProduct oldProduct, DJIBaseProduct newProduct) {
        mProduct = newProduct;
        Log.d(TAG, "onProductChanged");
        if (mProduct != null) {
            Log.d(TAG, "onProductChanged not null");
            mProduct.setDJIBaseProductListener(mDJIBaseProductListener);
        }
        notifyStatusChange();
    }

    private void notifyStatusChange() {
        statusListener.onStatusChanged();
    }

    @Nullable
    @Override
    public DJIFlightController getFlightController() {
        return flightController;
    }

    private DJIFlightController flightController;

    private DJIBaseProduct.DJIBaseProductListener mDJIBaseProductListener = new DJIBaseProduct.DJIBaseProductListener() {
        @Override
        public void onComponentChange(DJIBaseProduct.DJIComponentKey key, DJIBaseComponent oldComponent, DJIBaseComponent newComponent) {
            Log.d(TAG, "Component changed " + key.name());
            if (newComponent != null) {
                newComponent.setDJIComponentListener(mDJIComponentListener);
            }
            if (key == DJIBaseProduct.DJIComponentKey.FlightController) {
                Log.d(TAG, "Got flight controller");
                flightController = (DJIFlightController) newComponent;
            }
            notifyStatusChange();
        }

        @Override
        public void onProductConnectivityChanged(boolean isConnected) {
            notifyStatusChange();
        }
    };

    private DJIBaseComponent.DJIComponentListener mDJIComponentListener = new DJIBaseComponent.DJIComponentListener() {
        @Override
        public void onComponentConnectivityChanged(boolean b) {
            notifyStatusChange();
        }
    };


    public interface CopterStatusChangeListener {
        void onStatusChanged();
    }
}

