package fi.oulu.mapcopter.event;

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
