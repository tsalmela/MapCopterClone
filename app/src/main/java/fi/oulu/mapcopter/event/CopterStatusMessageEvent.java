package fi.oulu.mapcopter.event;

public class CopterStatusMessageEvent {
    private String message = "";

    public CopterStatusMessageEvent(String message) {
        this.message = message;
    }

    public CopterStatusMessageEvent() {
    }

    public String getMessage() {
        return message;
    }
}
