package fi.oulu.mapcopter.copter;

public interface AircraftPositionChangeListener {
    void onAircraftPositionChanged(double latitude, double longitude, float altitude);
}
