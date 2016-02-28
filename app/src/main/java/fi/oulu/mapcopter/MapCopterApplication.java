package fi.oulu.mapcopter;

import android.app.Application;

import com.squareup.otto.Bus;

import fi.oulu.mapcopter.copter.CopterManager;

public class MapCopterApplication extends Application {

    private static CopterManager copterManager;
    private static Bus defaultBus = new Bus();

    public static CopterManager getCopterManager() {
        return copterManager;
    }

    public static Bus getDefaultBus() {
        return defaultBus;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        MapCopterApplication.copterManager = CopterManager.createManager(this, defaultBus);
        MapCopterApplication.copterManager.initManager();
    }
}
