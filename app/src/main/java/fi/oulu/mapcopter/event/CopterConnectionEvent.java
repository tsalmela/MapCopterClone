package fi.oulu.mapcopter.event;

public class CopterConnectionEvent {
    private boolean isConnected;

    public CopterConnectionEvent(boolean isConnected) {
        this.isConnected = isConnected;
    }

    public boolean isConnected() {
        return isConnected;
    }
}
