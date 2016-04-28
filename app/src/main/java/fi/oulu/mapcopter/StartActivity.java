package fi.oulu.mapcopter;

import android.support.v7.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.support.v7.widget.Toolbar;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.ProgressBar;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.ClipDrawable;
import android.view.Gravity;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Handler;


import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;

import butterknife.ButterKnife;
import butterknife.OnClick;
import dji.sdk.FlightController.DJIFlightControllerDataType;
import fi.oulu.mapcopter.event.CopterConnectionEvent;

public class StartActivity extends AppCompatActivity {

    private static final String TAG = StartActivity.class.getSimpleName();
    private Bus eventBus;

    private static final int PROGRESS = 0x1;

    private ProgressBar mProgress;
    private int mProgressStatus = 50;

    private Handler mHandler = new Handler();

    private boolean gpsStatusCheckRunning = true;

    public int getGpsSignalStatus() {
        return MapCopterApplication.getCopterManager().getGPSStatus();
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);
        ButterKnife.bind(this);

        eventBus = MapCopterApplication.getDefaultBus();

        Button startButton = (Button) findViewById(R.id.buttonStart);
        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent myIntent = new Intent(StartActivity.this, MapActivity.class);
                StartActivity.this.startActivity(myIntent);
                MapCopterApplication.getCopterManager().takeOff();

            }
        });

        mProgress = (ProgressBar) findViewById(R.id.progress);

        new Thread(new Runnable() {
            public void run() {

                while (gpsStatusCheckRunning) {
                    int status = getGpsSignalStatus();
                    if (status == 255) {
                        status = 0;
                    } else {
                        status = status + 1;
                    }

                    final int progress = status * 20;

                    mHandler.post(new Runnable() {
                        public void run() {
                            mProgress.setProgress(progress);
                        }
                    });

                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();

    }

    @Override
    protected void onStart() {
        super.onStart();
        eventBus.register(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        eventBus.unregister(this);
    }

    @Subscribe
    public void onCopterConnectionChanged(CopterConnectionEvent event) {
        if (event.isConnected()) {
            TextView mInfo = (TextView) findViewById(R.id.textView_Connection);
            if (event.getModel() != null && !event.getModel().isEmpty()) {
                mInfo.setText(event.getModel());
            } else {
                mInfo.setText(R.string.activity_start_text_connected);
            }
        } else {
            TextView mInfo = (TextView) findViewById(R.id.textView_Connection);
            mInfo.setText(R.string.activity_start_text_not_connected);
        }
    }

    @OnClick(R.id.button_calibrate)
    public void onCalibrateClicked() {
        Log.d(TAG, "Calibrate button clicked");
        Intent intent = new Intent(this, CalibrationDialogActivity.class);
        startActivity(intent);
    }

    @OnClick(R.id.button_remoteAccessInfo)
    public void onRemoteInfoClicked(){
        Log.d(TAG, "onRemoteInfoClicked: ");
        Intent intent = new Intent(this, RemoteInfoDialogActivity.class);
        startActivity(intent);
    }
}
