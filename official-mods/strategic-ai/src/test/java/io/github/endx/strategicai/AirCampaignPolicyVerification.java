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
        require(AirCampaignPolicy.select(true, 10.0F, 0.0F, true)
                        == StrategicAirPlan.Mode.STRIKE,
                "a safe high-value strike was not released");
        require(AirCampaignPolicy.select(true, 10.0F, 0.0F, false)
                        == StrategicAirPlan.Mode.PATROL,
                "idle air superiority did not return to patrol");
        System.out.println("Strategic AI air campaign policy contracts passed");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
