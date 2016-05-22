package fi.oulu.mapcopter.copter;

/**
 * Interface for receiving aircraft position changes
 *
 * see {@link CopterManager#setCopterPositionChangeListener(AircraftPositionChangeListener)}
 */
public interface AircraftPositionChangeListener {
    /**
     * Called from the main UI thread when the aircraft position changes
     */
    void onAircraftPositionChanged(double latitude, double longitude, float altitude, double rotation);
}
