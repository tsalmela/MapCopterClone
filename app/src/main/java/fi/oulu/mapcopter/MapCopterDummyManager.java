package fi.oulu.mapcopter;

import dji.sdk.FlightController.DJIFlightController;
import dji.sdk.base.DJIBaseProduct;

public class MapCopterDummyManager extends MapCopterManager {
    @Override
    public DJIBaseProduct getProduct() {
        return null;
    }

    @Override
    public void initManager() {

    }

    @Override
    public DJIFlightController getFlightController() {
        return null;
    }
}
