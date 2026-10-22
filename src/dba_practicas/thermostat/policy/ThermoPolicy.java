package dba_practicas.thermostat.policy;

public class ThermoPolicy {

    // Action codes
    public static final String ACTION_ON = "thermostat.refrigeration.on";
    public static final String ACTION_OFF = "thermostat.refrigeration.off";

    // Information needed to decide
    private final double lower;
    private final double upper;
    private final double target;

    public ThermoPolicy(double lower, double upper, double target) {
        this.lower = lower;
        this.upper = upper;
        this.target = target;
    }

    // Condition-action rule: too warm -> cool, otherwise stop cooling
    public String decide(double temperature) {
        if (temperature > target) {
            return ACTION_ON;
        } else {
            return ACTION_OFF;
        }
    }
}