package fi.oulu.mapcopter;

import dji.sdk.base.DJIBaseProduct;

public class MapCopterDummyManager extends MapCopterManager {
    @Override
    public DJIBaseProduct getProduct() {
        return null;
    }

    @Override
    public void initManager() {

    }
}
