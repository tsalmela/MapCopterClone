package fi.oulu.mapcopter;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.util.Log;

import dji.sdk.FlightController.DJIFlightController;
import com.google.android.gms.maps.model.LatLng;
import com.squareup.otto.Bus;

import dji.sdk.SDKManager.DJISDKManager;
import dji.sdk.base.DJIBaseComponent;
import dji.sdk.base.DJIBaseProduct;
import dji.sdk.base.DJIError;
import dji.sdk.base.DJISDKError;

public class MapCopterRealManager extends MapCopterManager implements DJISDKManager.DJISDKManagerCallback {
    private static final String TAG = MapCopterRealManager.class.getSimpleName();

    private final Bus eventBus;

    private final Context context;
    private DJIBaseProduct mProduct;

    public MapCopterRealManager(Context context, Bus eventBus) {
        this.context = context;
        this.eventBus = eventBus;
    }

    @Override
    public DJIBaseProduct getProduct() {
        if (mProduct == null) {
            Log.w(TAG, "Trying to get product but it is null");
            mProduct = DJISDKManager.getInstance().getDJIProduct();
        }
        return mProduct;
    }

    @Override
    public void initManager() {
        DJISDKManager.getInstance()
                .initSDKManager(context, this);
    }

    @Override
    public void moveToPos(LatLng position) {

    }

    @Override
    public void onGetRegisteredResult(DJIError error) {
        if (error == DJISDKError.REGISTRATION_SUCCESS) {
            Log.i(TAG, "onGetRegisteredResult registration success");
            boolean connectionSucceeded = DJISDKManager.getInstance().startConnectionToProduct();
            if (!connectionSucceeded) {
                eventBus.post(new CopterStatusChangeEvent("Failed to connect to product"));
            }
        } else {
            Log.e(TAG, "onGetRegisteredResult: registration not success " + error.getDescription());
            eventBus.post(new CopterStatusChangeEvent("DJI App registration failed " + error.getDescription()));
        }
    }

    @Override
    public void onProductChanged(DJIBaseProduct oldProduct, DJIBaseProduct newProduct) {
        mProduct = newProduct;
        Log.d(TAG, "onProductChanged");
        if (mProduct != null) {
            mProduct.setDJIBaseProductListener(mDJIBaseProductListener);
            eventBus.post(new CopterStatusChangeEvent("Product connected: " + mProduct.getModel().getDisplayName()));
        } else {
            Log.w(TAG, "onProductChange new product is null");
            eventBus.post(new CopterStatusChangeEvent("Product disconnected"));
        }
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
        public void onComponentChange(final DJIBaseProduct.DJIComponentKey key, DJIBaseComponent oldComponent, DJIBaseComponent newComponent) {
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
                Log.d(TAG, "Component: " + key.name() + " connected");
                newComponent.setDJIComponentListener(new DJIBaseComponent.DJIComponentListener() {
                    @Override
                    public void onComponentConnectivityChanged(boolean isConnected) {
                        Log.d(TAG, "Connectivity changed for " + key.name() + " to " + isConnected);
                    }
                });
            } else {
                Log.w(TAG, "Component: " + key.name() + " disconnected");
            }
        }

        @Override
        public void onProductConnectivityChanged(boolean isConnected) {
            Log.d(TAG, "onProductConnectivityChanged to " + isConnected);
            // TODO: notify
        }
    };
}

