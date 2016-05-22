package fi.oulu.mapcopter.view;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.google.android.gms.maps.SupportMapFragment;

/**
 * A drop-in replacement for SupportMapFragment that wraps the map in a frame layout
 * in order to handle touch events. Used to listen for mapTouchStart and mapTouchEnd events.
 *
 * Usage:
 *   Replace your SupportMapFragment inside your layout xml with this
 *   Implement the {@link TouchListener} interface and
 *   Call {@link #setTouchListener(TouchListener)} to set the listener for touch start/end
 */
public class TouchableMapFragment extends SupportMapFragment {

    private View mContentView;
    private TouchableWrapper mWrapperView;
    private TouchListener touchListener;

    public void setTouchListener(TouchListener onTouchListener) {
        this.touchListener = onTouchListener;
        if (mWrapperView != null) {
            mWrapperView.setTouchListener(onTouchListener);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup parent,
                             Bundle savedInstanceState) {
        mContentView = super.onCreateView(inflater, parent,
                savedInstanceState);

        mWrapperView = new TouchableWrapper(getActivity());
        mWrapperView.addView(mContentView);
        mWrapperView.setTouchListener(touchListener);

        return mWrapperView;
    }

    @Override
    public View getView() {
        return mContentView;
    }

    public interface TouchListener {
        void onMapTouchStart();

        void onMapTouchEnd();
    }

    static class TouchableWrapper extends FrameLayout {
        @Nullable
        private TouchListener onTouchListener;

        public TouchableWrapper(Context context) {
            super(context);
        }

        public void setTouchListener(@Nullable TouchListener onTouchListener) {
            this.onTouchListener = onTouchListener;
        }

        @Override
        public boolean dispatchTouchEvent(MotionEvent event) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    if (onTouchListener != null) {
                        onTouchListener.onMapTouchStart();
                    }
                    break;
                case MotionEvent.ACTION_UP:
                    if (onTouchListener != null) {
                        onTouchListener.onMapTouchEnd();
                    }
                    break;
            }

            return super.dispatchTouchEvent(event);
        }
    }
}