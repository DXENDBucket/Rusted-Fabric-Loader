package io.github.endx.strategicai;

/** Hysteresis for releasing air-to-ground production after a sustained air lead. */
final class AirSuperiorityGate {
    static final int REQUIRED_OBSERVATIONS = 48;
    private int superiorityObservations;

    StrategicProductionDoctrine.AirBalance update(
            StrategicProductionDoctrine.AirBalance observed) {
        if (observed == StrategicProductionDoctrine.AirBalance.SUPERIORITY) {
            superiorityObservations = Math.min(
                    REQUIRED_OBSERVATIONS, superiorityObservations + 1);
            return superiorityObservations >= REQUIRED_OBSERVATIONS
                    ? StrategicProductionDoctrine.AirBalance.SUPERIORITY
                    : StrategicProductionDoctrine.AirBalance.PARITY;
        }
        superiorityObservations = 0;
        return observed;
    }

    int observations() { return superiorityObservations; }
}
