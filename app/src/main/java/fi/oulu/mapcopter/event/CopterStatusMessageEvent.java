package fi.oulu.mapcopter.event;

/**
 * Event used to deliver status messages from CopterManager to the activity to show to the user.
 */
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
