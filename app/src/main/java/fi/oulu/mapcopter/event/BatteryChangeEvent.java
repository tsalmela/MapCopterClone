package fi.oulu.mapcopter.event;

/**
 * Created by elmerimerenheimo on 21.4.2016.
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
