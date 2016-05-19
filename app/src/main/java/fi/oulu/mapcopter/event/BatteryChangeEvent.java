package fi.oulu.mapcopter.event;

public class BatteryChangeEvent {
    private final int batteryPercentage;

    public BatteryChangeEvent(int batteryEnergyRemainingPercent) {
        this.batteryPercentage = batteryEnergyRemainingPercent;
    }

    public int getBatteryPercentage() {
        return batteryPercentage;
    }
}
