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
 * in order to handle touch events
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
        void onTouchStart();

        void onTouchEnd();
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
                        onTouchListener.onTouchStart();
                    }
                    break;
                case MotionEvent.ACTION_UP:
                    if (onTouchListener != null) {
                        onTouchListener.onTouchEnd();
                    }
                    break;
            }

            return super.dispatchTouchEvent(event);
        }
    }
}