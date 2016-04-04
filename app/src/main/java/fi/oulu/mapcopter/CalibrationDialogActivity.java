package fi.oulu.mapcopter;

import android.os.AsyncTask;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import dji.sdk.FlightController.DJICompass;
import fi.oulu.mapcopter.copter.CopterManager;

import static dji.sdk.FlightController.DJICompass.DJICompassCalibrationStatus.*;

public class CalibrationDialogActivity extends AppCompatActivity {
    private static final String TAG = CalibrationDialogActivity.class.getSimpleName();


    @Bind(R.id.text_tutorial)
    TextView tutorialText;

    private CopterManager copterManager;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(getString(R.string.calibration_title));
        setContentView(R.layout.calibration_dialog);
        ButterKnife.bind(this);


        copterManager = MapCopterApplication.getCopterManager();
        copterManager.startCompassCalibration();
        new CalibrationStatusChecker().execute();
    }

    public void showHorizontalTutorial() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                tutorialText.setText(R.string.calibration_step1);
            }
        });
    }

    public void showVerticalTutorial() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                tutorialText.setText(R.string.calibration_step2);
            }
        });
    }


    private void showCalibrationFinished(boolean success) {
        if (success) {
            tutorialText.setText(R.string.calibration_finished_success);
        } else {
            tutorialText.setText(R.string.calibration_finished_failure);
        }
    }

    @OnClick(R.id.button_cancel)
    public void onCancelClicked() {
        finish();
    }

    class CalibrationStatusChecker extends AsyncTask<Void, Void, Boolean> {
        @Override
        protected Boolean doInBackground(Void... params) {
            int compassStatus = copterManager.getCompassStatus();

            Log.d(TAG, "Started calibration status checker");

            while (compassStatus != Failed.value()
                    && compassStatus != Succeeded.value()) {
                compassStatus = copterManager.getCompassStatus();

                Log.d(TAG, "Compass status is " + compassStatus);
                if (compassStatus == Horizontal.value()) {
                    Log.d(TAG, "Showing step 1");
                    showHorizontalTutorial();
                }
                if (compassStatus == Vertical.value()) {
                    Log.d(TAG, "Showing step 2");
                    showVerticalTutorial();
                }
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            copterManager.stopCompassCalibration();

            Log.d(TAG, "Calibration status: " + compassStatus);

            if (compassStatus == Succeeded.value()) {
                return true;
            } else {
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean success) {
            showCalibrationFinished(success);
        }
    }

}
