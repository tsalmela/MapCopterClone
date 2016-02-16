package fi.oulu.mapcopter;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.SurfaceTexture;
import android.os.Looper;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.TextureView;
import android.view.View;
import android.widget.Toast;

import dji.sdk.AirLink.DJILBAirLink;
import dji.sdk.Camera.DJICamera;
import dji.sdk.Codec.DJICodecManager;
import dji.sdk.SDKManager.DJISDKManager;
import dji.sdk.base.DJIBaseComponent;
import dji.sdk.base.DJIBaseProduct;
import dji.sdk.base.DJIError;
import dji.sdk.base.DJISDKError;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class MapActivity extends AppCompatActivity implements TextureView.SurfaceTextureListener {
    private static final String TAG = MapActivity.class.getSimpleName();

    private static final int UI_ANIMATION_DELAY = 500;
    private static final String FLAG_CONNECTION_CHANGE = "fi_oulu_mapcopter_connection_change";

    //private View mContentView;
    private Handler mHandler;


    private static DJIBaseProduct mProduct;

    private DJISDKManager.DJISDKManagerCallback mDJISDKManagerCallback = new DJISDKManager.DJISDKManagerCallback() {
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
                        Toast.makeText(getApplicationContext(),
                                "DJI SDK registration failed, check network connection",
                                Toast.LENGTH_LONG)
                                .show();
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
    };

    private DJIBaseProduct.DJIBaseProductListener mDJIBaseProductListener = new DJIBaseProduct.DJIBaseProductListener() {
        @Override
        public void onComponentChange(DJIBaseProduct.DJIComponentKey key, DJIBaseComponent oldComponent, DJIBaseComponent newComponent) {
            if (newComponent != null) {
                newComponent.setDJIComponentListener(mDJIComponentListener);
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

    private Runnable updateRunnable = new Runnable() {
        @Override
        public void run() {
            Intent intent = new Intent(FLAG_CONNECTION_CHANGE);
            Log.d(TAG, "update runnable");
            sendBroadcast(intent);
        }
    };
    private TextureView mCameraView;
    private DJICamera.CameraReceivedVideoDataCallback mReceivedVideoCallback;
    private DJICodecManager mCodecManager;
    private DJILBAirLink.DJIOnReceivedVideoCallback mOnReceivedVideoCallback;
    private DJICamera mCamera;

    private void notifyStatusChange() {
        mHandler.removeCallbacks(updateRunnable);
        mHandler.postDelayed(updateRunnable, 500);
    }

    public static synchronized DJIBaseProduct getProductInstance() {
        if (mProduct == null) {
            Log.d(TAG, "getProductInstance");
            mProduct = DJISDKManager.getInstance().getDJIProduct();
        } else {
            Log.d(TAG, "getProductInstance: else");
        }
        return mProduct;
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.d(TAG, "onCreate");

        mHandler = new Handler(Looper.getMainLooper());
        setContentView(R.layout.activity_map);
        mCameraView = (TextureView) findViewById(R.id.camera_view);

        mCameraView.setSurfaceTextureListener(this);

        //SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
        //        .findFragmentById(R.id.map);
        //mapFragment.getMapAsync(this);

        mReceivedVideoCallback = new DJICamera.CameraReceivedVideoDataCallback() {
            @Override
            public void onResult(byte[] videoBuffer, int size) {
                Log.d(TAG, "got video");
                if (mCodecManager != null) {
                    mCodecManager.sendDataToDecoder(videoBuffer, size);
                } else {
                    Log.e(TAG, "codec manager null");

                }
            }
        };

        mOnReceivedVideoCallback = new DJILBAirLink.DJIOnReceivedVideoCallback() {
            @Override
            public void onResult(byte[] videoBuffer, int size) {
                Log.d(TAG, "received video");
                if (mCodecManager != null) {
                    mCodecManager.sendDataToDecoder(videoBuffer, size);
                } else {
                    Log.e(TAG, "on receivd videoojdsclccallback error");
                }
            }
        };

        DJISDKManager.getInstance().initSDKManager(this, mDJISDKManagerCallback);

        IntentFilter filter = new IntentFilter();
        filter.addAction(FLAG_CONNECTION_CHANGE);
        registerReceiver(mReceiver, filter);
    }

    protected BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            onProductChange();
        }
    };

    private void onProductChange() {
        initPreviewer();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mReceiver);
    }

    @Override
    protected void onResume() {
        super.onResume();
        initPreviewer();
        if (mCameraView == null) {
            Log.e(TAG, "video surface null");
        }
    }

    private void initPreviewer() {
        Log.d(TAG, "initPreviewer");
        try {
            mProduct = getProductInstance();
            Log.d(TAG, "initPreviewer: mProduct " + mProduct);
        } catch (Exception exception) {
            mProduct = null;
            Log.e(TAG, "initPreviewer: exception ", exception);
        }

        if (mProduct == null || !mProduct.isConnected()) {
            mCamera = null;
            Log.e(TAG, "kalamiehen virhe " + mProduct);
        } else {
            Log.d(TAG, "initPreviewer: product connected not null");
            if (mCameraView != null) {
                mCameraView.setSurfaceTextureListener(this);
            }

            if (!mProduct.getModel().equals(DJIBaseProduct.Model.UnknownAircraft)) {
                mCamera = mProduct.getCamera();
                if (mCamera != null) {
                    mCamera.setDJICameraReceivedVideoDataCallback(mReceivedVideoCallback);
                }
            } else {
                if (mProduct.getAirLink() != null) {
                    if (mProduct.getAirLink().getLBAirLink() != null) {
                        mProduct.getAirLink().getLBAirLink().setDJIOnReceivedVideoCallback(mOnReceivedVideoCallback);
                    }
                }
            }
        }
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        // Trigger the initial hide() shortly after the activity has been
        // created, to briefly hint to the user that UI controls
        // are available.
        delayedHide(UI_ANIMATION_DELAY);
    }

    @SuppressLint("InlinedApi")
    private void hide() {
        /*
        mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
                */
    }

    @SuppressLint("InlinedApi")
    private void show() {
        // Show the system bar
        /*
        mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
                */
    }

    private final Handler mHideHandler = new Handler();
    private final Runnable mHideRunnable = new Runnable() {
        @Override
        public void run() {
            hide();
        }
    };

    /**
     * Schedules a call to hide() in [delay] milliseconds, canceling any
     * previously scheduled calls.
     */
    private void delayedHide(int delayMillis) {
        mHideHandler.removeCallbacks(mHideRunnable);
        mHideHandler.postDelayed(mHideRunnable, delayMillis);
    }

    /*@Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Add a marker and move the camera
        LatLng lipasto = new LatLng(65.0591, 25.466549);
        mMap.addMarker(new MarkerOptions().position(lipasto).title("Lipasto"));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(lipasto));
    }*/

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        if (mCodecManager == null) {
            Log.e(TAG, "codecmanager null");
            mCodecManager = new DJICodecManager(this, surface, width, height);
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
}
