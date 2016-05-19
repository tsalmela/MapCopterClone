package fi.oulu.mapcopter.remote;

import android.annotation.SuppressLint;
import android.os.Build;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import fi.oulu.mapcopter.remote.view.TouchableMapFragment;

public class MapActivity extends AppCompatActivity {
    private static final String TAG = MapActivity.class.getSimpleName();
    private View mContentView;

    private View mControlsView;

    private void setFullScreenMode() {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        } else {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setFullScreenMode();
        setContentView(R.layout.activity_map);

//        mControlsView = findViewById(R.id.fullscreen_content_controls);
        mContentView = findViewById(R.id.fullscreen_content);

        getSupportFragmentManager()
                .beginTransaction()
                .add(R.id.fullscreen_content, new TouchableMapFragment())
                .commit();

        getSupportFragmentManager()
                .beginTransaction()
                .add(R.id.small_window_content, new VideoFragment())
                .commit();

        View smallWindow = findViewById(R.id.small_window_click_interceptor);
        if (smallWindow != null) {
            smallWindow.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    switchWindows();
                }
            });
        }
    }

    private void switchWindows() {
        Log.d(TAG, "Switching windows");
        FragmentManager fm = getSupportFragmentManager();
        Fragment small = fm.findFragmentById(R.id.small_window_content);
        Fragment full = fm.findFragmentById(R.id.fullscreen_content);
        fm.beginTransaction()
                .remove(small)
                .remove(full)
                .commit();
        fm.executePendingTransactions();
        fm.beginTransaction()
                .add(R.id.small_window_content, full)
                .add(R.id.fullscreen_content, small)
                .commit();
    }
}
