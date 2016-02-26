package fi.oulu.mapcopter;

import android.app.Application;

import com.squareup.otto.Bus;

public class MapCopter extends Application {

    private static MapCopterManager mapCopterManager;
    private static Bus defaultBus = new Bus();

    public static MapCopterManager getMapCopterManager() {
        return mapCopterManager;
    }

    public static Bus getDefaultBus() {
        return defaultBus;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        MapCopter.mapCopterManager = MapCopterManager.createManager(this, defaultBus);
        MapCopter.mapCopterManager.initManager();
    }
}
