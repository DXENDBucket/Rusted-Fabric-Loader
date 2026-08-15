package io.github.endx.strategicai;

public final class FrontEngagementPolicyVerification {
    private FrontEngagementPolicyVerification() {
    }

    public static void main(String[] args) {
        require(FrontEngagementPolicy.select(2.0F, 8, 0)
                        == StrategicFrontState.Mode.OPEN,
                "an open front was treated as a tower line");
        require(FrontEngagementPolicy.select(0.4F, 8, 2)
                        == StrategicFrontState.Mode.ATTRITION,
                "a losing tower engagement was allowed to rush");
        require(FrontEngagementPolicy.select(0.9F, 5, 2)
                        == StrategicFrontState.Mode.MUSTER,
                "a plausible attack was not held for assembly");
        require(FrontEngagementPolicy.select(1.6F, 7, 2)
                        == StrategicFrontState.Mode.ASSAULT,
                "a favorable assembled force was not released");
        System.out.println("Strategic AI front engagement policy contracts passed");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
