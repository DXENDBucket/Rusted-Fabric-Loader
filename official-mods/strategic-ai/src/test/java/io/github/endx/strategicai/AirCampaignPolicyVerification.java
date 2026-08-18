package io.github.endx.strategicai;

public final class AirCampaignPolicyVerification {
    private AirCampaignPolicyVerification() {
    }

    public static void main(String[] args) {
        require(AirCampaignPolicy.select(false, 10.0F, 0.0F, true)
                        == StrategicAirPlan.Mode.REGROUP,
                "a scattered air force launched a strike");
        require(AirCampaignPolicy.select(true, 7.0F, 10.0F, false)
                        == StrategicAirPlan.Mode.REGROUP,
                "an air-inferior force accepted an isolated interception");
        require(AirCampaignPolicy.select(true, 9.0F, 10.0F, true)
                        == StrategicAirPlan.Mode.INTERCEPT,
                "enemy air was ignored for a ground strike");
        require(AirCampaignPolicy.select(false, 12.0F, 8.0F, true)
                        == StrategicAirPlan.Mode.INTERCEPT,
                "a clear air lead waited passively while enemy aircraft remained");
        require(AirCampaignPolicy.select(true, 10.0F, 0.0F, true)
                        == StrategicAirPlan.Mode.STRIKE,
                "a safe high-value strike was not released");
        require(AirCampaignPolicy.select(true, 10.0F, 0.0F, false)
                        == StrategicAirPlan.Mode.PATROL,
                "idle air superiority did not return to patrol");
        AirSuperiorityGate gate = new AirSuperiorityGate();
        for (int index = 1; index < AirSuperiorityGate.REQUIRED_OBSERVATIONS; index++) {
            require(gate.update(StrategicProductionDoctrine.AirBalance.SUPERIORITY)
                            == StrategicProductionDoctrine.AirBalance.PARITY,
                    "a transient air lead released bombers early");
        }
        require(gate.update(StrategicProductionDoctrine.AirBalance.SUPERIORITY)
                        == StrategicProductionDoctrine.AirBalance.SUPERIORITY,
                "a sustained air lead never released air-to-ground production");
        require(gate.update(StrategicProductionDoctrine.AirBalance.PARITY)
                        == StrategicProductionDoctrine.AirBalance.PARITY
                        && gate.observations() == 0,
                "losing the air lead did not reset bomber authorization");
        System.out.println("Strategic AI air campaign policy contracts passed");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
