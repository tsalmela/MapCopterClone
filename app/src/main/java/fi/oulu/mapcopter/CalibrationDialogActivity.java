package fi.oulu.mapcopter;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.annotation.UiThread;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.TextView;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import fi.oulu.mapcopter.copter.CopterManager;

import static dji.sdk.FlightController.DJICompass.DJICompassCalibrationStatus.Failed;
import static dji.sdk.FlightController.DJICompass.DJICompassCalibrationStatus.Horizontal;
import static dji.sdk.FlightController.DJICompass.DJICompassCalibrationStatus.Succeeded;
import static dji.sdk.FlightController.DJICompass.DJICompassCalibrationStatus.Vertical;

public class CalibrationDialogActivity extends AppCompatActivity {
    private static final String TAG = CalibrationDialogActivity.class.getSimpleName();
    private static final int CALIBRATION_HORIZONTAL_STEP = 1;
    private static final int CALIBRATION_VERTICAL_STEP = 2;

    @Bind(R.id.text_tutorial)
    TextView tutorialText;

    private CopterManager copterManager;
    private boolean forceStop = false;
    private int currentCalibrationStep = 0;

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

    @Override
    protected void onStop() {
        super.onStop();
        forceStop = true;
    }

    @Override
    protected void onStart() {
        super.onStart();
        forceStop = false;
    }

    public void showHorizontalTutorial() {
        if (currentCalibrationStep != CALIBRATION_HORIZONTAL_STEP) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    tutorialText.setText(R.string.calibration_step1);
                    currentCalibrationStep = CALIBRATION_HORIZONTAL_STEP;
                }
            });
        }
    }

    public void showVerticalTutorial() {
        if (currentCalibrationStep != CALIBRATION_VERTICAL_STEP) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    tutorialText.setText(R.string.calibration_step2);
                    currentCalibrationStep = CALIBRATION_VERTICAL_STEP;
                }
            });
        }
    }


    @UiThread
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

            while (!forceStop
                    && compassStatus != Failed.value()
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
                    forceStop = true;
                }
            }

            copterManager.stopCompassCalibration();

            Log.d(TAG, "Calibration stopped, status: " + compassStatus);

            return compassStatus == Succeeded.value();
        }

        @Override
        protected void onPostExecute(Boolean success) {
            showCalibrationFinished(success);
        }
    }

}
