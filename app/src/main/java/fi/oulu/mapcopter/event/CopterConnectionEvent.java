package fi.oulu.mapcopter.event;

/**
 * Event used to indicate a change in the copter connectivity.
 */
public class CopterConnectionEvent {
    private boolean isConnected;
    private String model;

    public CopterConnectionEvent(boolean isConnected, String model) {
        this.isConnected = isConnected;
        this.model = model;
    }

    public String getModel() {
        return model;
    }

    public boolean isConnected() {
        return isConnected;
    }
}
