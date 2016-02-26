package fi.oulu.mapcopter;

public class CopterStatusChangeEvent {
    private String message = "";

    public CopterStatusChangeEvent(String message) {
        this.message = message;
    }

    public CopterStatusChangeEvent() {
    }

    public String getMessage() {
        return message;
    }
}
