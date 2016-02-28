package fi.oulu.mapcopter.event;

import android.graphics.SurfaceTexture;

public class VideoSurfaceAvailableEvent {
    private final SurfaceTexture surface;
    private final int width;
    private final int height;
    private boolean isAvailable;

    public static VideoSurfaceAvailableEvent surfaceNotAvailable() {
        VideoSurfaceAvailableEvent event = new VideoSurfaceAvailableEvent(null, 0, 0);
        event.isAvailable = false;
        return event;
    }

    public VideoSurfaceAvailableEvent(SurfaceTexture surface, int width, int height) {
        isAvailable = true;
        this.surface = surface;
        this.width = width;
        this.height = height;
    }

    public boolean isAvailble() {
        return isAvailable;
    }

    public SurfaceTexture getSurface() {
        return surface;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }
}
