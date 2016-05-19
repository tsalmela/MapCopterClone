package fi.oulu.mapcopter.event;

import android.os.Handler;
import android.os.Looper;

import com.squareup.otto.Bus;

/**
 * subclass of otto event bus that posts all events in the main UI thread
 */
public class MainThreadEventBus extends Bus {

    private Handler mainThreadHandler;

    public MainThreadEventBus(Handler mainThreadHandler) {
        this.mainThreadHandler = mainThreadHandler;
    }

    @Override
    public void post(final Object event) {
        if (Thread.currentThread() == Looper.getMainLooper().getThread()) {
            super.post(event);
        } else {
            mainThreadHandler.post(new Runnable() {
                @Override
                public void run() {
                    MainThreadEventBus.super.post(event);
                }
            });
        }
    }
}
