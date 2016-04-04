package fi.oulu.mapcopter.copter;

public interface AircraftPositionChangeListener {
    /**
     * Called from the main UI thread when the aircraft position changes
     */
    void onAircraftPositionChanged(double latitude, double longitude, float altitude, double rotation);
}
