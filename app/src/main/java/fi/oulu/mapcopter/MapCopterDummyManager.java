package fi.oulu.mapcopter;

import com.google.android.gms.maps.model.LatLng;

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
    public void moveToPos(LatLng position) {

    }
}
