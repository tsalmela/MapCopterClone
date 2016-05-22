package fi.oulu.mapcopter.event;

import fi.oulu.mapcopter.MapCopterApplication;

/**
 * {@link fi.oulu.mapcopter.copter.DJICopterManager DJICopterManager}  sends this event to the
 * {@link MapCopterApplication#getDefaultBus() default event bus} every time the remaining
 * battery percentage changes.
 */
public class BatteryChangeEvent {
    private final int batteryPercentage;

    public BatteryChangeEvent(int batteryEnergyRemainingPercent) {
        this.batteryPercentage = batteryEnergyRemainingPercent;
    }

    public int getBatteryPercentage() {
        return batteryPercentage;
    }
}
